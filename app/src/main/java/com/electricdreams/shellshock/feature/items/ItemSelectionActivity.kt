package com.electricdreams.shellshock.feature.items

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electricdreams.shellshock.R
import com.electricdreams.shellshock.core.util.BasketManager
import com.electricdreams.shellshock.core.util.CurrencyManager
import com.electricdreams.shellshock.core.util.ItemManager
import com.electricdreams.shellshock.core.worker.BitcoinPriceWorker
import com.electricdreams.shellshock.feature.items.adapters.SelectionBasketAdapter
import com.electricdreams.shellshock.feature.items.adapters.SelectionItemsAdapter
import com.electricdreams.shellshock.feature.items.handlers.BasketUIHandler
import com.electricdreams.shellshock.feature.items.handlers.CheckoutHandler
import com.electricdreams.shellshock.feature.items.handlers.ItemSearchHandler
import com.electricdreams.shellshock.feature.items.handlers.SelectionAnimationHandler

/**
 * Activity for selecting items and adding them to a basket for checkout.
 * Supports search, quantity adjustments, custom variations, and checkout flow.
 */
class ItemSelectionActivity : AppCompatActivity() {

    // ----- Managers -----
    private lateinit var itemManager: ItemManager
    private lateinit var basketManager: BasketManager
    private lateinit var bitcoinPriceWorker: BitcoinPriceWorker
    private lateinit var currencyManager: CurrencyManager

    // ----- Views -----
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var searchInput: EditText
    private lateinit var scanButton: ImageButton
    private lateinit var clearFiltersButton: ImageButton
    private lateinit var categoryBadge: TextView
    private lateinit var categoryChipsContainer: FlexboxLayout
    private lateinit var basketSection: LinearLayout
    private lateinit var basketRecyclerView: RecyclerView
    private lateinit var basketTotalView: TextView
    private lateinit var clearBasketButton: TextView
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var noResultsView: LinearLayout
    private lateinit var checkoutContainer: CardView
    private lateinit var checkoutButton: Button
    private lateinit var topBar: LinearLayout
    
    // Empty state views
    private lateinit var emptyStateFullscreen: FrameLayout
    private var emptyStateAnimator: EmptyStateAnimator? = null

    // ----- Category State -----
    private var categoryChipViews: MutableMap<String, TextView> = mutableMapOf()

    // ----- Adapters -----
    private lateinit var itemsAdapter: SelectionItemsAdapter
    private lateinit var basketAdapter: SelectionBasketAdapter

    // ----- Handlers -----
    private lateinit var animationHandler: SelectionAnimationHandler
    private lateinit var basketUIHandler: BasketUIHandler
    private lateinit var searchHandler: ItemSearchHandler
    private lateinit var checkoutHandler: CheckoutHandler

    // ----- Activity Result Launchers -----
    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == CheckoutScannerActivity.RESULT_BASKET_UPDATED) {
            refreshBasket()
            itemsAdapter.notifyDataSetChanged()
        }
    }
    
    private val addItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Reload items and update UI
            searchHandler.loadItems()
            updateEmptyStateVisibility()
        }
    }
    
    private val csvPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Import CSV file
            importCsvFile(uri)
        }
    }

    // ----- Lifecycle -----

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_selection)

        initializeManagers()
        initializeViews()
        initializeHandlers()
        initializeAdapters()
        setupRecyclerViews()
        setupClickListeners()
        setupEmptyStateButtons()

        // Load initial data
        searchHandler.loadItems()
        updateEmptyStateVisibility()
        refreshBasket()

        bitcoinPriceWorker.start()
    }

    override fun onResume() {
        super.onResume()
        // Refresh in case items were modified
        searchHandler.loadItems()
        updateEmptyStateVisibility()
        refreshBasket()
        
        // Start animation if empty state is visible
        if (emptyStateFullscreen.visibility == View.VISIBLE) {
            emptyStateAnimator?.start()
        }
    }
    
    override fun onPause() {
        super.onPause()
        emptyStateAnimator?.stop()
    }

    // ----- Initialization -----

    private fun initializeManagers() {
        itemManager = ItemManager.getInstance(this)
        basketManager = BasketManager.getInstance()
        bitcoinPriceWorker = BitcoinPriceWorker.getInstance(this)
        currencyManager = CurrencyManager.getInstance(this)
    }

    private fun initializeViews() {
        findViewById<View>(R.id.back_button).setOnClickListener { finish() }

        mainScrollView = findViewById(R.id.main_scroll_view)
        searchInput = findViewById(R.id.search_input)
        scanButton = findViewById(R.id.scan_button)
        clearFiltersButton = findViewById(R.id.clear_filters_button)
        categoryBadge = findViewById(R.id.category_badge)
        categoryChipsContainer = findViewById(R.id.category_chips_container)
        basketSection = findViewById(R.id.basket_section)
        basketRecyclerView = findViewById(R.id.basket_recycler_view)
        basketTotalView = findViewById(R.id.basket_total)
        clearBasketButton = findViewById(R.id.clear_basket_button)
        itemsRecyclerView = findViewById(R.id.items_recycler_view)
        noResultsView = findViewById(R.id.no_results_view)
        checkoutContainer = findViewById(R.id.checkout_container)
        checkoutButton = findViewById(R.id.checkout_button)
        topBar = findViewById(R.id.top_bar)
        
        // Empty state views
        emptyStateFullscreen = findViewById(R.id.empty_state_fullscreen)
    }

    private fun initializeHandlers() {
        animationHandler = SelectionAnimationHandler(
            basketSection = basketSection,
            checkoutContainer = checkoutContainer
        )

        basketUIHandler = BasketUIHandler(
            basketManager = basketManager,
            currencyManager = currencyManager,
            basketTotalView = basketTotalView,
            checkoutButton = checkoutButton,
            animationHandler = animationHandler,
            onBasketUpdated = { basketAdapter.updateItems(basketManager.getBasketItems()) }
        )

        // Create a dummy LinearLayout for the emptyView parameter since we handle it differently now
        val dummyEmptyView = LinearLayout(this)
        
        searchHandler = ItemSearchHandler(
            itemManager = itemManager,
            searchInput = searchInput,
            itemsRecyclerView = itemsRecyclerView,
            emptyView = dummyEmptyView, // We handle empty state ourselves
            noResultsView = noResultsView,
            onItemsFiltered = { items -> itemsAdapter.updateItems(items) },
            onFilterStateChanged = { hasActiveFilters -> updateFilterButtonState(hasActiveFilters) }
        )

        checkoutHandler = CheckoutHandler(
            activity = this,
            basketManager = basketManager,
            currencyManager = currencyManager,
            bitcoinPriceWorker = bitcoinPriceWorker
        )
    }

    private fun initializeAdapters() {
        itemsAdapter = SelectionItemsAdapter(
            context = this,
            basketManager = basketManager,
            mainScrollView = mainScrollView,
            onQuantityChanged = { refreshBasket() },
            onQuantityAnimation = { quantityView -> animationHandler.animateQuantityChange(quantityView) }
        )

        basketAdapter = SelectionBasketAdapter(
            currencyManager = currencyManager,
            onItemRemoved = { itemId -> handleItemRemoved(itemId) }
        )
    }

    private fun setupRecyclerViews() {
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = itemsAdapter

        basketRecyclerView.layoutManager = LinearLayoutManager(this)
        basketRecyclerView.adapter = basketAdapter
    }

    private fun setupClickListeners() {
        scanButton.setOnClickListener {
            val intent = Intent(this, CheckoutScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }

        clearFiltersButton.setOnClickListener {
            clearAllFilters()
        }

        clearBasketButton.setOnClickListener {
            showClearBasketDialog()
        }

        checkoutButton.setOnClickListener {
            checkoutHandler.proceedToCheckout()
        }

        // Setup search focus listener to show/hide category chips
        searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && searchHandler.hasCategories()) {
                showCategoryChips()
            }
        }

        // Category badge click to remove category filter
        categoryBadge.setOnClickListener {
            selectCategory(null)
        }
    }
    
    private fun setupEmptyStateButtons() {
        // Find buttons in the included empty state layout
        val emptyView = findViewById<View>(R.id.empty_view)
        val addButton = emptyView?.findViewById<Button>(R.id.empty_state_add_button)
        val importButton = emptyView?.findViewById<View>(R.id.empty_state_import_button)
        val closeButton = emptyView?.findViewById<ImageButton>(R.id.empty_state_close_button)
        val ribbonContainer = emptyView?.findViewById<View>(R.id.ribbon_container)

        addButton?.setOnClickListener {
            val intent = Intent(this, ItemEntryActivity::class.java)
            addItemLauncher.launch(intent)
        }

        importButton?.setOnClickListener {
            csvPickerLauncher.launch("text/csv")
        }
        
        closeButton?.setOnClickListener {
            finish()
        }

        // Initialize the animator
        ribbonContainer?.let {
            emptyStateAnimator = EmptyStateAnimator(this, it)
        }
    }
    
    private fun importCsvFile(uri: android.net.Uri) {
        try {
            val tempFile = java.io.File(cacheDir, "import_catalog.csv")
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                android.widget.Toast.makeText(this, "Failed to open CSV file", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            val importedCount = itemManager.importItemsFromCsv(tempFile.absolutePath, true)

            if (importedCount > 0) {
                android.widget.Toast.makeText(this, "Imported $importedCount items", android.widget.Toast.LENGTH_SHORT).show()
                searchHandler.loadItems()
                updateEmptyStateVisibility()
            } else {
                android.widget.Toast.makeText(this, "No items imported from CSV", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: java.io.IOException) {
            android.util.Log.e("ItemSelectionActivity", "Error importing CSV file: ${e.message}", e)
            android.widget.Toast.makeText(this, "Error importing CSV file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Update visibility of full-screen empty state vs main content.
     */
    private fun updateEmptyStateVisibility() {
        val hasItems = searchHandler.getAllItems().isNotEmpty()
        
        if (hasItems) {
            // Show main content
            emptyStateFullscreen.visibility = View.GONE
            mainScrollView.visibility = View.VISIBLE
            emptyStateAnimator?.stop()
            
            // Reset navigation bar to normal
            setNavigationBarStyle(isDarkBackground = false)
        } else {
            // Show full-screen empty state
            emptyStateFullscreen.visibility = View.VISIBLE
            mainScrollView.visibility = View.GONE
            emptyStateAnimator?.start()
            
            // Set navigation bar to match dark background
            setNavigationBarStyle(isDarkBackground = true)
        }
    }
    
    /**
     * Set the navigation bar style to match the current screen background.
     */
    private fun setNavigationBarStyle(isDarkBackground: Boolean) {
        window.navigationBarColor = if (isDarkBackground) {
            ContextCompat.getColor(this, R.color.empty_state_background)
        } else {
            ContextCompat.getColor(this, R.color.color_bg_white)
        }
        
        // Set light/dark icons in navigation bar
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightNavigationBars = !isDarkBackground
        }
        
        // Also update status bar
        window.statusBarColor = if (isDarkBackground) {
            ContextCompat.getColor(this, R.color.empty_state_background)
        } else {
            ContextCompat.getColor(this, R.color.color_bg_white)
        }
        
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkBackground
        }
    }

    // ----- Actions -----

    private fun refreshBasket() {
        basketUIHandler.refreshBasket()
    }

    private fun handleItemRemoved(itemId: String) {
        basketManager.removeItem(itemId)
        itemsAdapter.resetItemQuantity(itemId)
        refreshBasket()
    }

    private fun showClearBasketDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Basket")
            .setMessage("Remove all items from basket?")
            .setPositiveButton("Clear") { _, _ ->
                basketManager.clearBasket()
                itemsAdapter.clearAllQuantities()
                refreshBasket()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ----- Category Filtering -----

    /**
     * Build and populate the category chips from available categories.
     */
    private fun buildCategoryChips() {
        categoryChipsContainer.removeAllViews()
        categoryChipViews.clear()

        val categories = searchHandler.getCategories()
        if (categories.isEmpty()) return

        val inflater = LayoutInflater.from(this)
        val chipSpacingH = resources.getDimensionPixelSize(R.dimen.space_s)
        val chipSpacingV = resources.getDimensionPixelSize(R.dimen.space_xs)

        categories.forEach { category ->
            val chip = inflater.inflate(R.layout.item_category_chip, categoryChipsContainer, false) as TextView
            chip.text = category
            chip.isSelected = false

            // Add margin between chips using FlexboxLayout.LayoutParams
            val params = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, chipSpacingH, chipSpacingV)
            }
            chip.layoutParams = params

            chip.setOnClickListener {
                val isCurrentlySelected = chip.isSelected
                if (isCurrentlySelected) {
                    selectCategory(null)
                } else {
                    selectCategory(category)
                }
            }

            categoryChipsContainer.addView(chip)
            categoryChipViews[category] = chip
        }
    }

    /**
     * Show category chips with animation.
     */
    private fun showCategoryChips() {
        if (!searchHandler.hasCategories()) return

        buildCategoryChips()
        updateCategoryChipSelection()

        if (categoryChipsContainer.visibility == View.VISIBLE) return

        categoryChipsContainer.alpha = 0f
        categoryChipsContainer.translationY = -20f
        categoryChipsContainer.visibility = View.VISIBLE

        val fadeIn = ObjectAnimator.ofFloat(categoryChipsContainer, "alpha", 0f, 1f)
        val slideDown = ObjectAnimator.ofFloat(categoryChipsContainer, "translationY", -20f, 0f)

        AnimatorSet().apply {
            playTogether(fadeIn, slideDown)
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * Hide category chips with animation.
     */
    private fun hideCategoryChips() {
        if (categoryChipsContainer.visibility != View.VISIBLE) return

        val fadeOut = ObjectAnimator.ofFloat(categoryChipsContainer, "alpha", 1f, 0f)
        val slideUp = ObjectAnimator.ofFloat(categoryChipsContainer, "translationY", 0f, -20f)

        AnimatorSet().apply {
            playTogether(fadeOut, slideUp)
            duration = 150
            interpolator = DecelerateInterpolator()
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    categoryChipsContainer.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    /**
     * Select a category to filter by.
     */
    private fun selectCategory(category: String?) {
        searchHandler.setSelectedCategory(category)
        updateCategoryBadge(category)
        updateCategoryChipSelection()
    }

    /**
     * Update the category badge in the search bar.
     */
    private fun updateCategoryBadge(category: String?) {
        if (category != null) {
            categoryBadge.text = category
            categoryBadge.visibility = View.VISIBLE

            // Animate badge appearance
            categoryBadge.alpha = 0f
            categoryBadge.scaleX = 0.8f
            categoryBadge.scaleY = 0.8f

            val fadeIn = ObjectAnimator.ofFloat(categoryBadge, "alpha", 0f, 1f)
            val scaleX = ObjectAnimator.ofFloat(categoryBadge, "scaleX", 0.8f, 1f)
            val scaleY = ObjectAnimator.ofFloat(categoryBadge, "scaleY", 0.8f, 1f)

            AnimatorSet().apply {
                playTogether(fadeIn, scaleX, scaleY)
                duration = 150
                interpolator = DecelerateInterpolator()
                start()
            }
        } else {
            categoryBadge.visibility = View.GONE
        }
    }

    /**
     * Update the visual selection state of category chips.
     */
    private fun updateCategoryChipSelection() {
        val selectedCategory = searchHandler.getSelectedCategory()

        categoryChipViews.forEach { (category, chip) ->
            val isSelected = category == selectedCategory
            chip.isSelected = isSelected

            if (isSelected) {
                chip.setTextColor(ContextCompat.getColor(this, R.color.color_bg_white))
            } else {
                chip.setTextColor(ContextCompat.getColor(this, R.color.color_text_primary))
            }
        }
    }

    /**
     * Update the scan/clear button visibility based on filter state.
     */
    private fun updateFilterButtonState(hasActiveFilters: Boolean) {
        if (hasActiveFilters) {
            if (scanButton.visibility == View.VISIBLE) {
                // Cross-fade transition
                scanButton.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withEndAction {
                        scanButton.visibility = View.GONE
                        clearFiltersButton.visibility = View.VISIBLE
                        clearFiltersButton.alpha = 0f
                        clearFiltersButton.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        } else {
            if (clearFiltersButton.visibility == View.VISIBLE) {
                // Cross-fade transition
                clearFiltersButton.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withEndAction {
                        clearFiltersButton.visibility = View.GONE
                        scanButton.visibility = View.VISIBLE
                        scanButton.alpha = 0f
                        scanButton.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        }
    }

    /**
     * Clear all active filters.
     */
    private fun clearAllFilters() {
        searchHandler.clearAllFilters()
        updateCategoryBadge(null)
        updateCategoryChipSelection()
        hideCategoryChips()
        searchInput.clearFocus()
        
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }
}
