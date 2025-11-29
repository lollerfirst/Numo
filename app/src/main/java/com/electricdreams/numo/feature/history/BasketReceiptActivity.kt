    /**
     * Calculate total fiat value including converted sats items.
     */
    private fun getTotalFiatIncludingSatsConversion(): Long {
        val b = basket
        if (b == null) {
            return enteredAmount
        }
        
        val fiatTotal = b.getFiatGrossTotalCents()
        val satsItems = b.getSatsItems()
        
        val price = bitcoinPrice
        if (satsItems.isEmpty() || price == null || price <= 0) {
            return fiatTotal
        }
        
        // Convert sats items to fiat
        val satsTotal = b.getSatsDirectTotal()
        val satsInFiat = ((satsTotal.toDouble() / 100_000_000.0) * price * 100).toLong()
        
        return fiatTotal + satsInFiat
    }
