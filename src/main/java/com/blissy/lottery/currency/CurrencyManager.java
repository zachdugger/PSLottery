package com.blissy.lottery.currency;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages all currency types for the lottery system.
 */
public class CurrencyManager {
    private final Map<String, Currency> currencies = new HashMap<>();

    /**
     * Register a currency with the manager.
     * @param currency The currency to register
     */
    public void registerCurrency(Currency currency) {
        currencies.put(currency.getId(), currency);
    }

    /**
     * Get a currency by its ID.
     * @param id The currency ID
     * @return The currency, or empty if not found
     */
    public Optional<Currency> getCurrency(String id) {
        return Optional.ofNullable(currencies.get(id));
    }

    /**
     * Get all registered currencies.
     * @return Collection of currencies
     */
    public Collection<Currency> getAllCurrencies() {
        return currencies.values();
    }

    /**
     * Check if a currency with the given ID exists.
     * @param id The currency ID
     * @return True if the currency exists, false otherwise
     */
    public boolean hasCurrency(String id) {
        return currencies.containsKey(id);
    }

    /**
     * Get the number of registered currencies.
     * @return The number of currencies
     */
    public int getCurrencyCount() {
        return currencies.size();
    }
}