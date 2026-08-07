package com.example.urlshortener.user.service;

/**
 * Phase 2 placeholder retained only as historical reference; Phase 3 ownership
 * is supplied by SecurityCurrentOwnerProvider.
 */
public class PlaceholderCurrentOwnerProvider implements CurrentOwnerProvider {

    @Override
    public OwnerIdentity getCurrentOwner() {
        throw new UnsupportedOperationException("Placeholder owner provider is not active");
    }
}
