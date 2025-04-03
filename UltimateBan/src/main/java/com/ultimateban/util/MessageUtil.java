package com.ultimateban.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling message formatting and color codes
 */
public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([A-Fa-f0-9]{6}):([A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static boolean supportsHex = false;

    /**
     * Initialize the MessageUtil class
     * Checks if the server version supports hex colors
     */
    public static void init() {
        try {
            // Check if the server supports RGB colors (1.16+)
            Class.forName("net.md_5.bungee.api.ChatColor").getMethod("of", String.class);
            supportsHex = true;
        } catch (Exception e) {
            supportsHex = false;
        }
    }

    /**
     * Translate color codes in a message
     *
     * @param message The message to colorize
     * @return The colorized message
     */
    public static String color(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Translate color codes in a list of messages
     *
     * @param messages The list of messages to colorize
     * @return The list of colorized messages
     */
    public static List<String> color(List<String> messages) {
        List<String> coloredMessages = new ArrayList<>();
        for (String message : messages) {
            coloredMessages.add(color(message));
        }
        return coloredMessages;
    }

    /**
     * Send a colorized message to a command sender
     *
     * @param sender The recipient
     * @param message The message to send
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(color(message));
        }
    }

    /**
     * Send colorized messages to a command sender
     *
     * @param sender The recipient of the messages
     * @param messages The messages to send
     */
    public static void sendMessage(CommandSender sender, List<String> messages) {
        if (sender != null && messages != null && !messages.isEmpty()) {
            messages.forEach(message -> sendMessage(sender, message));
        }
    }

    /**
     * Replace placeholders in a message
     *
     * @param message The message containing placeholders
     * @param placeholders The placeholders and their values (in pairs: placeholder1, value1, placeholder2, value2, ...)
     * @return The message with replaced placeholders
     */
    public static String replacePlaceholders(String message, String... placeholders) {
        if (message == null || placeholders == null || placeholders.length % 2 != 0) {
            return message;
        }

        String result = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            
            // Skip if either the placeholder or value is null
            if (placeholder == null || value == null) {
                continue;
            }
            
            // Add % to placeholder if not present
            if (!placeholder.startsWith("%")) {
                placeholder = "%" + placeholder;
            }
            if (!placeholder.endsWith("%")) {
                placeholder = placeholder + "%";
            }
            
            result = result.replace(placeholder, value);
        }
        
        return result;
    }

    /**
     * Send a title to a player
     *
     * @param player The player to send the title to
     * @param title The title text
     * @param subtitle The subtitle text
     * @param fadeIn The fade in time in ticks
     * @param stay The stay time in ticks
     * @param fadeOut The fade out time in ticks
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        
        player.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
    }

    /**
     * Process gradient syntax in a message
     * @param message The message to process
     * @return The message with gradients applied
     */
    private static String processGradients(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String text = matcher.group(3);
            
            ChatColor startColor = ChatColor.of("#" + startHex);
            ChatColor endColor = ChatColor.of("#" + endHex);
            
            int length = text.length();
            StringBuilder gradientText = new StringBuilder();
            
            for (int i = 0; i < length; i++) {
                float ratio = (float) i / (length - 1);
                
                int r = (int) (getRed(startColor) * (1 - ratio) + getRed(endColor) * ratio);
                int g = (int) (getGreen(startColor) * (1 - ratio) + getGreen(endColor) * ratio);
                int b = (int) (getBlue(startColor) * (1 - ratio) + getBlue(endColor) * ratio);
                
                ChatColor color = ChatColor.of(String.format("#%02X%02X%02X", r, g, b));
                gradientText.append(color).append(text.charAt(i));
            }
            
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradientText.toString()));
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    private static int getRed(ChatColor color) {
        return color.getColor().getRed();
    }
    
    private static int getGreen(ChatColor color) {
        return color.getColor().getGreen();
    }
    
    private static int getBlue(ChatColor color) {
        return color.getColor().getBlue();
    }
    
    /**
     * Create a fancy message with hover and click events
     * @param text The text to show
     * @param hoverText The text to show when hovering
     * @param command The command to execute when clicked
     * @return The fancy text component
     */
    public static TextComponent createFancyMessage(String text, String hoverText, String command) {
        TextComponent message = new TextComponent(color(text));
        
        if (hoverText != null && !hoverText.isEmpty()) {
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder(color(hoverText)).create()));
        }
        
        if (command != null && !command.isEmpty()) {
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        }
        
        return message;
    }
    
    /**
     * Broadcast a message to all players
     * @param message The message to broadcast
     */
    public static void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(color(message)));
        Bukkit.getConsoleSender().sendMessage(color(message));
    }
    
    /**
     * Broadcast a fancy message to all players
     * @param component The text component to broadcast
     */
    public static void broadcast(TextComponent component) {
        Bukkit.getOnlinePlayers().forEach(player -> player.spigot().sendMessage(component));
        Bukkit.getConsoleSender().sendMessage(component.toLegacyText());
    }
    
    /**
     * Colorize a list of strings
     * @param strings The strings to colorize
     * @return The colorized strings
     */
    public static List<String> colorList(List<String> strings) {
        List<String> colorizedStrings = new ArrayList<>();
        for (String string : strings) {
            colorizedStrings.add(color(string));
        }
        return colorizedStrings;
    }
} 