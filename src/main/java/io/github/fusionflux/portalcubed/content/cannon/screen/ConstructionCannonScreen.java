package io.github.fusionflux.portalcubed.content.cannon.screen;

import io.github.fusionflux.portalcubed.PortalCubed;
import io.github.fusionflux.portalcubed.content.cannon.data.CannonSettings;
import io.github.fusionflux.portalcubed.content.cannon.screen.tab.MaterialsTab;
import io.github.fusionflux.portalcubed.content.cannon.screen.widget.ConstructPreviewWidget;
import io.github.fusionflux.portalcubed.content.cannon.screen.widget.TabWidget;
import io.github.fusionflux.portalcubed.framework.gui.layout.PanelLayout;
import io.github.fusionflux.portalcubed.framework.gui.widget.TexturedStickyButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Locale;

public class ConstructionCannonScreen extends Screen {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 120;
	public static final int BACKGROUND_Y_OFFSET = TabWidget.HEIGHT - 4; // tabs are supposed to slightly overlap the top
	public static final int TAB_TITLE_Y_OFFSET = BACKGROUND_Y_OFFSET + 5;
	public static final int TAB_TITLE_X_OFFSET = 14;

	public static final Component TITLE = Component.translatable("container.portalcubed.construction_cannon");
	public static final ResourceLocation BACKGROUND = PortalCubed.id("textures/gui/container/construction_cannon/materials_tab.png");

	private final CannonDataHolder settings;

	private Tab tab;

	public ConstructionCannonScreen(CannonSettings settings) {
		super(TITLE);
		this.settings = new CannonDataHolder(settings);
		this.tab = Tab.MATERIALS;
	}

	@Override
	protected void init() {
		super.init();
		LinearLayout root = LinearLayout.horizontal();
		root.defaultCellSetting().paddingHorizontal(10).alignVerticallyMiddle();

		ConstructPreviewWidget preview = root.addChild(new ConstructPreviewWidget(120));

		LinearLayout rightSide = root.addChild(LinearLayout.vertical());
		rightSide.defaultCellSetting().alignHorizontallyCenter().paddingVertical(5);

		PanelLayout menu = rightSide.addChild(new PanelLayout());

		LinearLayout tabs = LinearLayout.horizontal();
		for (int i = 0; i < Tab.values().length; i++) {
			Tab tab = Tab.values()[i];
			TabWidget button = new TabWidget(tab, () -> this.switchToTab(tab));
			if (tab == this.tab) {
				button.select();
			}
			tabs.addChild(button);
			tabs.addChild(SpacerElement.width(1)); // 1-pixel buffer
		}

		menu.addChild(TAB_TITLE_X_OFFSET, TAB_TITLE_Y_OFFSET, new StringWidget(this.tab.title, this.font))
				.setColor(4210752); // magic number from InventoryScreen
		menu.addChild(0, BACKGROUND_Y_OFFSET, ImageWidget.texture(WIDTH, HEIGHT, BACKGROUND, 256, 256));
		// add tabs after so they're on top
		menu.addChild(0, 0, tabs);

		switch (this.tab) {
			case MATERIALS -> MaterialsTab.init(this.settings, menu);
			case CONSTRUCTS -> {}
			case SETTINGS -> {}
		}

		// cannon view, temporary placeholder
		rightSide.addChild(new ConstructPreviewWidget(60));

		// first arrangement, set bounds
		root.arrangeElements();
		// center whole thing
		int xOff = (this.width - root.getWidth()) / 2;
		int yOff = (this.height - root.getHeight()) / 2;
		root.setPosition(xOff, yOff);
		// second arrangement, apply new position
		root.arrangeElements();
		root.visitWidgets(this::addRenderableWidget);
		// reverse order of children so they iterate highest to lowest
		Collections.reverse(this.children());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void switchToTab(Tab tab) {
		if (this.tab != tab) {
			this.tab = tab;
			this.rebuildWidgets();
		}
	}

	private void save() {
		this.onClose();
	}

	public static Component translate(String key) {
		return Component.translatable("container.portalcubed.construction_cannon." + key);
	}

	public enum Tab {
		MATERIALS, CONSTRUCTS, SETTINGS;

		public final String name = this.name().toLowerCase(Locale.ROOT);
		public final Component title = translate("tab." + this.name);
		public final TexturedStickyButton.Textures textures = TexturedStickyButton.Textures.noHover(
				PortalCubed.id("construction_cannon/tab_" + this.name + "_unselected"),
				PortalCubed.id("construction_cannon/tab_" + this.name + "_selected")
		);
	}
}