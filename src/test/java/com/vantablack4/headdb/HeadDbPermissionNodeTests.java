package com.vantablack4.headdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import org.junit.jupiter.api.Test;

final class HeadDbPermissionNodeTests {
    @Test
    void publishesExactLuckPermsCompatibleFabricPermissionNodes() {
        Map<PermissionNode<Boolean>, String> nodes = Map.of(
            HeadDbCommands.GIVE_REMOTE_PERMISSION, "vantablack.command.agivehead",
            HeadDbCommands.GIVE_PLAYER_PERMISSION, "vantablack.command.agiveplayerhead",
            HeadDbCommands.REFRESH_PERMISSION, "vantablack.command.arefreshheaddb",
            HeadDbCommands.VERIFY_PERMISSION, "vantablack.command.averifyheaddb",
            HeadDbCommands.STATUS_PERMISSION, "vantablack.command.aheaddbstatus"
        );

        assertThat(nodes).hasSize(5);
        nodes.forEach((node, expected) -> assertThat(
            node.key().getNamespace() + "." + node.key().getPath()
        ).isEqualTo(expected));
    }
}
