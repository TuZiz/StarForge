package ym.starforge.integration;

public final class PapiBridge {
    public static PlaceholderCache create(){var expansion=new ForgePlaceholders();expansion.register();return expansion;}
}
