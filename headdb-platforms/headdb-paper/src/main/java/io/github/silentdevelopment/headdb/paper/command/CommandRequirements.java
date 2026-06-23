package io.github.silentdevelopment.headdb.paper.command;

import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.core.requirement.PredicateCommandRequirement;
import io.github.silentdevelopment.relay.text.CommandText;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public final class CommandRequirements {

    private static final String NO_PERMISSION_KEY = "command.error.no-permission";
    private static final String NO_PERMISSION_FALLBACK = "<red>You do not have permission to do that.";

    private CommandRequirements() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull PredicateCommandRequirement<CommandSender> permission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");

        if (permission.isBlank()) {
            throw new IllegalArgumentException("permission cannot be blank.");
        }

        return new PredicateCommandRequirement<>(
                sender -> sender.hasPermission(Permissions.ADMIN) || sender.hasPermission(permission),
                CommandText.keyed(NO_PERMISSION_KEY, NO_PERMISSION_FALLBACK, Map.of("permission", permission))
        );
    }
}