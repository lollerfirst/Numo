package com.electricdreams.numo.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.electricdreams.numo.R;
import com.electricdreams.numo.core.util.MintIconCache;
import com.electricdreams.numo.core.util.MintManager;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Adapter for the list of allowed mints in settings
 */
public class MintsAdapter extends RecyclerView.Adapter<MintsAdapter.MintViewHolder> {
    
    private static final String TAG = "MintsAdapter";
    
    private List<String> mints;
    private final MintRemoveListener removeListener;
    private final LightningMintSelectedListener lightningListener;
    private final WithdrawListener withdrawListener;
    private String preferredLightningMint;
    private Map<String, Long> mintBalances = new HashMap<>();
    private Map<String, Boolean> loadingStates = new HashMap<>();
    private MintManager mintManager;
    
    /**
     * Interface for handling mint removal
     */
    public interface MintRemoveListener {
        void onMintRemoved(String mintUrl);
    }
    
    /**
     * Interface for handling Lightning mint selection
     */
    public interface LightningMintSelectedListener {
        void onLightningMintSelected(String mintUrl);
    }
    
    /**
     * Interface for handling withdraw button clicks
     */
    public interface WithdrawListener {
        void onWithdrawClicked(String mintUrl, long balance);
    }
    
    public MintsAdapter(Context context, List<String> mints, MintRemoveListener listener) {
        this(context, mints, listener, null, null);
    }
    
    public MintsAdapter(Context context, List<String> mints, MintRemoveListener removeListener, 
                       @Nullable LightningMintSelectedListener lightningListener,
                       @Nullable String preferredLightningMint) {
        this(context, mints, removeListener, lightningListener, preferredLightningMint, null);
    }
    
    public MintsAdapter(Context context, List<String> mints, MintRemoveListener removeListener, 
                       @Nullable LightningMintSelectedListener lightningListener,
                       @Nullable String preferredLightningMint,
                       @Nullable WithdrawListener withdrawListener) {
        this.mints = mints;
        this.removeListener = removeListener;
        this.lightningListener = lightningListener;
        this.withdrawListener = withdrawListener;
        this.preferredLightningMint = preferredLightningMint;
        this.mintManager = MintManager.getInstance(context);
        
        // Initialize all mints as loading
        for (String mint : mints) {
            loadingStates.put(mint, true);
        }
    }
    
    /**
     * Set the preferred Lightning mint URL
     */
    public void setPreferredLightningMint(@Nullable String mintUrl) {
        this.preferredLightningMint = mintUrl;
        notifyDataSetChanged();
    }
    
    /**
     * Update the balance for a specific mint
     */
    public void setMintBalance(String mintUrl, long balance) {
        mintBalances.put(mintUrl, balance);
        loadingStates.put(mintUrl, false);
        int index = mints.indexOf(mintUrl);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }
    
    /**
     * Update all mint balances at once
     */
    public void setAllBalances(Map<String, Long> balances) {
        mintBalances.clear();
        mintBalances.putAll(balances);
        for (String mint : mints) {
            loadingStates.put(mint, false);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Set loading state for a mint
     */
    public void setLoading(String mintUrl, boolean loading) {
        loadingStates.put(mintUrl, loading);
        int index = mints.indexOf(mintUrl);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }
    
    /**
     * Set all mints to loading state
     */
    public void setAllLoading() {
        for (String mint : mints) {
            loadingStates.put(mint, true);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public MintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mint, parent, false);
        return new MintViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MintViewHolder holder, int position) {
        String mint = mints.get(position);
        holder.bind(mint);
    }
    
    @Override
    public int getItemCount() {
        return mints.size();
    }
    
    /**
     * Update the list of mints
     */
    public void updateMints(List<String> newMints) {
        this.mints = newMints;
        // Set new mints as loading
        for (String mint : newMints) {
            if (!loadingStates.containsKey(mint)) {
                loadingStates.put(mint, true);
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * Format balance in satoshis for display
     */
    private String formatBalance(long sats) {
        if (sats == 0) {
            return "₿0";
        } else if (sats >= 1_000_000) {
            // Show as millions with 2 decimal places
            double millions = sats / 1_000_000.0;
            return String.format("₿%.2fM", millions);
        } else if (sats >= 1_000) {
            // Show as thousands with 1 decimal place
            double thousands = sats / 1_000.0;
            return String.format("₿%.1fK", thousands);
        } else {
            return String.format("₿%,d", sats);
        }
    }
    
    /**
     * ViewHolder for mint items
     */
    class MintViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView mintIcon;
        private final TextView mintNameText;
        private final TextView mintUrlText;
        private final TextView mintBalanceText;
        private final ProgressBar balanceLoading;
        private final ImageButton removeButton;
        private final RadioButton lightningRadio;
        private final View withdrawButton;
        
        public MintViewHolder(@NonNull View itemView) {
            super(itemView);
            mintIcon = itemView.findViewById(R.id.mint_icon);
            mintNameText = itemView.findViewById(R.id.mint_name_text);
            mintUrlText = itemView.findViewById(R.id.mint_url_text);
            mintBalanceText = itemView.findViewById(R.id.mint_balance_text);
            balanceLoading = itemView.findViewById(R.id.balance_loading);
            removeButton = itemView.findViewById(R.id.remove_mint_button);
            lightningRadio = itemView.findViewById(R.id.lightning_mint_radio);
            withdrawButton = itemView.findViewById(R.id.withdraw_button);
        }
        
        public void bind(String mintUrl) {
            // Display mint name (from info if available, otherwise host)
            String displayName = mintManager.getMintDisplayName(mintUrl);
            mintNameText.setText(displayName);
            
            // Load and display mint icon
            loadMintIcon(mintUrl);
            
            // Display the full URL in monospace grey below
            mintUrlText.setText(mintUrl);
            
            // Display balance or loading state
            Boolean isLoading = loadingStates.get(mintUrl);
            if (isLoading != null && isLoading) {
                mintBalanceText.setText("Loading...");
                balanceLoading.setVisibility(View.VISIBLE);
            } else {
                Long balance = mintBalances.get(mintUrl);
                mintBalanceText.setText(balance != null ? formatBalance(balance) : "₿0");
                balanceLoading.setVisibility(View.GONE);
            }
            
            // Set up Lightning mint radio button
            boolean isPreferred = mintUrl.equals(preferredLightningMint);
            lightningRadio.setChecked(isPreferred);
            
            lightningRadio.setOnClickListener(v -> {
                if (lightningListener != null) {
                    lightningListener.onLightningMintSelected(mintUrl);
                }
            });
            
            // Also allow clicking the whole card to select as Lightning mint
            itemView.setOnClickListener(v -> {
                if (lightningListener != null) {
                    lightningListener.onLightningMintSelected(mintUrl);
                }
            });
            
            // Set up remove button
            removeButton.setOnClickListener(v -> {
                // Show confirmation dialog before removal
                Context context = itemView.getContext();
                new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(R.string.mints_settings_dialog_remove_title)
                    .setMessage(context.getString(R.string.mints_settings_dialog_remove_message, mintUrl))
                    .setPositiveButton(R.string.mints_settings_dialog_remove_positive, (dialog, which) -> {
                        if (removeListener != null) {
                            removeListener.onMintRemoved(mintUrl);
                        }
                    })
                    .setNegativeButton(R.string.common_cancel, null)
                    .show();
            });
            
            // Set up withdraw button
            Long balance = mintBalances.get(mintUrl);
            long balanceValue = balance != null ? balance : 0L;
            
            if (withdrawButton != null) {
                withdrawButton.setOnClickListener(v -> {
                    if (withdrawListener != null) {
                        withdrawListener.onWithdrawClicked(mintUrl, balanceValue);
                    }
                });
                
                // Hide withdraw button if balance is 0 or loading
                Boolean isCurrentlyLoading = loadingStates.get(mintUrl);
                boolean shouldShow = balanceValue > 0 && (isCurrentlyLoading == null || !isCurrentlyLoading);
                withdrawButton.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
            }
        }
        
        /**
         * Load and display the mint icon.
         * Checks cache first, then downloads if needed.
         */
        private void loadMintIcon(String mintUrl) {
            // Reset to default icon first
            mintIcon.setImageResource(R.drawable.ic_bitcoin);
            
            // Get icon URL from mint info
            String iconUrl = mintManager.getMintIconUrl(mintUrl);
            Log.d(TAG, "loadMintIcon for " + mintUrl + " -> iconUrl: " + iconUrl);
            
            if (iconUrl == null || iconUrl.isEmpty()) {
                // No icon URL available, keep default
                Log.d(TAG, "No icon URL for " + mintUrl + ", using default");
                return;
            }
            
            // Check if icon is already cached
            File cachedIcon = MintIconCache.INSTANCE.getCachedIconFile(mintUrl);
            if (cachedIcon != null && cachedIcon.exists()) {
                // Load from cache immediately
                Log.d(TAG, "Loading icon from cache: " + cachedIcon.getAbsolutePath());
                Bitmap bitmap = BitmapFactory.decodeFile(cachedIcon.getAbsolutePath());
                if (bitmap != null) {
                    mintIcon.setImageBitmap(bitmap);
                    Log.d(TAG, "Loaded icon from cache for " + mintUrl);
                    return;
                } else {
                    Log.w(TAG, "Failed to decode cached icon for " + mintUrl);
                }
            }
            
            // Download icon asynchronously using Thread
            Log.d(TAG, "Downloading icon for " + mintUrl + " from " + iconUrl);
            new Thread(() -> {
                try {
                    File iconFile = kotlinx.coroutines.BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, continuation) -> MintIconCache.INSTANCE.downloadAndCacheIcon(mintUrl, iconUrl, continuation)
                    );
                    
                    if (iconFile != null && iconFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
                        if (bitmap != null) {
                            Log.d(TAG, "Downloaded and loaded icon for " + mintUrl);
                            // Update UI on main thread
                            itemView.post(() -> mintIcon.setImageBitmap(bitmap));
                        } else {
                            Log.w(TAG, "Failed to decode downloaded icon for " + mintUrl);
                        }
                    } else {
                        Log.w(TAG, "Download returned null/non-existent file for " + mintUrl);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error downloading icon for " + mintUrl + ": " + e.getMessage());
                }
            }).start();
        }
    }
}
