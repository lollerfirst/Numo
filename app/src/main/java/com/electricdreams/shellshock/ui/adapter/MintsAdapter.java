package com.electricdreams.shellshock.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.electricdreams.shellshock.R;

import java.util.List;

/**
 * Adapter for the list of allowed mints in settings
 */
public class MintsAdapter extends RecyclerView.Adapter<MintsAdapter.MintViewHolder> {
    
    private List<String> mints;
    private final MintRemoveListener removeListener;
    private final LightningMintSelectedListener lightningListener;
    private String preferredLightningMint;
    
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
    
    public MintsAdapter(List<String> mints, MintRemoveListener listener) {
        this(mints, listener, null, null);
    }
    
    public MintsAdapter(List<String> mints, MintRemoveListener removeListener, 
                       @Nullable LightningMintSelectedListener lightningListener,
                       @Nullable String preferredLightningMint) {
        this.mints = mints;
        this.removeListener = removeListener;
        this.lightningListener = lightningListener;
        this.preferredLightningMint = preferredLightningMint;
    }
    
    /**
     * Set the preferred Lightning mint URL
     */
    public void setPreferredLightningMint(@Nullable String mintUrl) {
        this.preferredLightningMint = mintUrl;
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
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder for mint items
     */
    class MintViewHolder extends RecyclerView.ViewHolder {
        private final TextView mintUrlText;
        private final ImageButton removeButton;
        private final RadioButton lightningRadio;
        
        public MintViewHolder(@NonNull View itemView) {
            super(itemView);
            mintUrlText = itemView.findViewById(R.id.mint_url_text);
            removeButton = itemView.findViewById(R.id.remove_mint_button);
            lightningRadio = itemView.findViewById(R.id.lightning_mint_radio);
        }
        
        public void bind(String mintUrl) {
            mintUrlText.setText(mintUrl);
            
            // Set up Lightning mint radio button
            boolean isPreferred = mintUrl.equals(preferredLightningMint);
            lightningRadio.setChecked(isPreferred);
            
            lightningRadio.setOnClickListener(v -> {
                if (lightningListener != null) {
                    lightningListener.onLightningMintSelected(mintUrl);
                }
            });
            
            // Also allow clicking the whole row to select as Lightning mint
            itemView.setOnClickListener(v -> {
                if (lightningListener != null) {
                    lightningListener.onLightningMintSelected(mintUrl);
                }
            });
            
            // Set up remove button
            removeButton.setOnClickListener(v -> {
                // Show confirmation dialog before removal
                new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                    .setTitle("Remove Mint")
                    .setMessage("Are you sure you want to remove this mint?\n\n" + mintUrl)
                    .setPositiveButton("Remove", (dialog, which) -> {
                        if (removeListener != null) {
                            removeListener.onMintRemoved(mintUrl);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }
}
