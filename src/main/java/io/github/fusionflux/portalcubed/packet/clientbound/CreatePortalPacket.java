package io.github.fusionflux.portalcubed.packet.clientbound;

import io.github.fusionflux.portalcubed.content.portal.Portal;
import io.github.fusionflux.portalcubed.content.portal.manager.ClientPortalManager;
import io.github.fusionflux.portalcubed.packet.ClientboundPacket;
import io.github.fusionflux.portalcubed.packet.PortalCubedPackets;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

import org.quiltmc.loader.api.minecraft.ClientOnly;

public class CreatePortalPacket implements ClientboundManagePortalsPacket {
	private final Portal portal;

	public CreatePortalPacket(Portal portal) {
		this.portal = portal;
	}

	public CreatePortalPacket(FriendlyByteBuf buf) {
		this.portal = Portal.fromNetwork(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		this.portal.toNetwork(buf);
	}

	@Override
	public PacketType<?> getType() {
		return PortalCubedPackets.CREATE_PORTAL;
	}

	@Override
	@ClientOnly
	public void handle(LocalPlayer player, ClientPortalManager manager, PacketSender responder) {
		manager.addPortal(this.portal);
	}
}
