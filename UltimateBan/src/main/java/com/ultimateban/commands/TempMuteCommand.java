package com.ultimateban.commands;

import com.ultimateban.UltimateBan;

/**
 * Command for temporary mutes
 */
public class TempMuteCommand extends MuteCommand {
    
    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public TempMuteCommand(UltimateBan plugin) {
        super(plugin, true);
    }
} 