package com.ultimateban.commands;

import com.ultimateban.UltimateBan;

/**
 * Command for temporary IP bans
 */
public class TempIpBanCommand extends IpBanCommand {
    
    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public TempIpBanCommand(UltimateBan plugin) {
        super(plugin, true);
    }
} 