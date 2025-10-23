package com.example.exceljson;

public class Config {
    public String outputVersion;
    public boolean attachAllUnitsIfNoMatch;
    public Sheets sheets = new Sheets();
    public MappingAliases aliases = new MappingAliases();
    public RecipientParsing recipientParsing = new RecipientParsing();

    public static class RecipientParsing {
        public String functionalRoleRegex;
    }
}
