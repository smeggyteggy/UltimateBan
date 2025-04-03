package com.ultimateban.commands;

import com.ultimateban.UltimateBan;

/**
 * Command for temporary bans
 */
public class TempBanCommand extends BanCommand {
    
    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public TempBanCommand(UltimateBan plugin) {
        super(plugin, true);
    }
} 