package com.example.exceljson;

public class Config {
    public String outputVersion = "1.0";
    public boolean attachAllUnitsIfNoMatch = true;
    public Sheets sheets = new Sheets();
    public MappingAliases aliases = new MappingAliases();
    public RecipientParsing recipientParsing = new RecipientParsing();

    public static class RecipientParsing {
        public String functionalRoleRegex = "^(?i)(vassign:.*|.*\\[room\\].*)$";
    }

    public static Config defaultConfig() {
        return new Config();
    }
}
