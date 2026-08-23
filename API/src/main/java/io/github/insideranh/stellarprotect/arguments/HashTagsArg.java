package io.github.insideranh.stellarprotect.arguments;

import lombok.Data;

@Data
public class HashTagsArg {

    private boolean preview;
    private boolean verbose;
    private boolean silent;
    private boolean count;
    private boolean session;
    private boolean entities;
    private boolean blocks;
    private boolean containers;
    private boolean items;
    private boolean kills;
    private boolean chunk;
    private boolean exportCsv;
    private boolean exportJson;
    private boolean undoMode;
    private boolean redo;
    private boolean regex;

    public HashTagsArg(String[] arguments) {
        for (String arg : arguments) {
            if (arg.equals("#preview")) this.preview = true;
            if (arg.equals("#verbose")) this.verbose = true;
            if (arg.equals("#silent")) this.silent = true;
            if (arg.equals("#count")) this.count = true;
            if (arg.equals("#session")) this.session = true;
            if (arg.equals("#entities")) this.entities = true;
            if (arg.equals("#blocks")) this.blocks = true;
            if (arg.equals("#containers")) this.containers = true;
            if (arg.equals("#items")) this.items = true;
            if (arg.equals("#kills")) this.kills = true;
            if (arg.equals("#chunk")) this.chunk = true;
            if (arg.equals("#export-csv")) this.exportCsv = true;
            if (arg.equals("#export-json")) this.exportJson = true;
            if (arg.equals("#undo")) this.undoMode = true;
            if (arg.equals("#redo")) this.redo = true;
            if (arg.equals("#regex")) this.regex = true;
        }
    }

    public boolean isRestoringOnlyBlocks() {
        return blocks && !containers && !items;
    }

    public boolean isRestoringOnlyContainers() {
        return containers && !blocks && !items;
    }

    public boolean isRestoringOnlyItems() {
        return items && !blocks && !containers;
    }

    public boolean isRestoringOnlyKills() {
        return kills && !blocks && !containers && !items;
    }

    public boolean hasAnyScope() {
        return blocks || containers || items || kills || entities;
    }

    public String getEffectiveScope() {
        if (isRestoringOnlyBlocks()) return "blocks";
        if (isRestoringOnlyContainers()) return "containers";
        if (isRestoringOnlyItems()) return "items";
        if (isRestoringOnlyKills()) return "kills";
        return "all";
    }

}