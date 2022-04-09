package com.simibubi.create.content.logistics.trains.management.edgePoint.station;

import java.lang.ref.WeakReference;
import java.util.List;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.logistics.trains.entity.Carriage;
import com.simibubi.create.content.logistics.trains.entity.Train;
import com.simibubi.create.content.logistics.trains.entity.TrainIconType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.networking.AllPackets;

import net.minecraft.client.gui.components.Widget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

public class AssemblyScreen extends AbstractStationScreen {

	private IconButton quitAssembly;
	private IconButton toggleAssemblyButton;
	private IconButton completeAssembly;

	private List<ResourceLocation> iconTypes;
	private ScrollInput iconTypeScroll;
	private boolean assemblyCompleted;

	public AssemblyScreen(StationTileEntity te, GlobalStation station) {
		super(te, station);
		background = AllGuiTextures.STATION_ASSEMBLING;
		assemblyCompleted = false;
	}

	@Override
	protected void init() {
		super.init();
		int x = guiLeft;
		int y = guiTop;
		int by = y + background.height - 24;

		Widget widget = renderables.get(0);
		if (widget instanceof IconButton ib) {
			ib.setIcon(AllIcons.I_PRIORITY_VERY_LOW);
			ib.setToolTip(new TextComponent("Close Window"));
		}

		iconTypes = TrainIconType.REGISTRY.keySet()
			.stream()
			.toList();
		iconTypeScroll = new ScrollInput(x + 4, y + 17, 184, 14).titled(new TextComponent("Icon Type"));
		iconTypeScroll.withRange(0, iconTypes.size());
		iconTypeScroll.withStepFunction(ctx -> -iconTypeScroll.standardStep()
			.apply(ctx));
		iconTypeScroll.calling(s -> {
			Train train = displayedTrain.get();
			if (train != null)
				train.icon = TrainIconType.byId(iconTypes.get(s));
		});
		iconTypeScroll.active = iconTypeScroll.visible = false;
		addRenderableWidget(iconTypeScroll);

		toggleAssemblyButton = new WideIconButton(x + 83, by, AllGuiTextures.I_ASSEMBLE_TRAIN);
		toggleAssemblyButton.active = false;
		toggleAssemblyButton.setToolTip(new TextComponent("Assemble Train"));
		toggleAssemblyButton.withCallback(() -> {
			AllPackets.channel.sendToServer(StationEditPacket.tryAssemble(te.getBlockPos()));
		});

		quitAssembly = new IconButton(x + 62, by, AllIcons.I_DISABLE);
		quitAssembly.active = true;
		quitAssembly.setToolTip(new TextComponent("Cancel Assembly"));
		quitAssembly.withCallback(() -> {
			AllPackets.channel.sendToServer(StationEditPacket.configure(te.getBlockPos(), false, station.name));
			minecraft.setScreen(new StationScreen(te, station));
		});

		completeAssembly = new IconButton(x + 112, by, AllIcons.I_CONFIRM);
		completeAssembly.active = false;
		completeAssembly.setToolTip(new TextComponent("Complete Assembly"));
		completeAssembly.withCallback(() -> {
			assemblyCompleted = true;
			AllPackets.channel.sendToServer(StationEditPacket.configure(te.getBlockPos(), false, station.name));
			minecraft.setScreen(new StationScreen(te, station));
		});

		addRenderableWidget(toggleAssemblyButton);
		addRenderableWidget(quitAssembly);
		addRenderableWidget(completeAssembly);

		tickTrainDisplay();
	}

	@Override
	public void tick() {
		super.tick();
		tickTrainDisplay();
		Train train = displayedTrain.get();
		toggleAssemblyButton.active = te.bogeyCount > 0 || train != null;
	}

	private void tickTrainDisplay() {
		Train train = displayedTrain.get();
		Train imminentTrain = getImminent();

		if (train == null) {
			if (imminentTrain != null) {
				displayedTrain = new WeakReference<>(imminentTrain);
				completeAssembly.active = true;
				quitAssembly.active = false;
				iconTypeScroll.active = iconTypeScroll.visible = true;
				toggleAssemblyButton.setToolTip(new TextComponent("Disassemble Train"));
				toggleAssemblyButton.setIcon(AllGuiTextures.I_DISASSEMBLE_TRAIN);
				toggleAssemblyButton.withCallback(() -> {
					AllPackets.channel.sendToServer(StationEditPacket.tryDisassemble(te.getBlockPos()));
				});
			}
		} else {
			if (imminentTrain == null) {
				displayedTrain = new WeakReference<>(null);
				completeAssembly.active = false;
				quitAssembly.active = true;
				iconTypeScroll.active = iconTypeScroll.visible = false;
				toggleAssemblyButton.setToolTip(new TextComponent("Assemble Train"));
				toggleAssemblyButton.setIcon(AllGuiTextures.I_ASSEMBLE_TRAIN);
				toggleAssemblyButton.withCallback(() -> {
					AllPackets.channel.sendToServer(StationEditPacket.tryAssemble(te.getBlockPos()));
				});
			}
		}
	}

	@Override
	protected void renderWindow(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
		super.renderWindow(ms, mouseX, mouseY, partialTicks);
		int x = guiLeft;
		int y = guiTop;

		TextComponent header = new TextComponent("Train Assembly");
		font.draw(ms, header, x + background.width / 2 - font.width(header) / 2, y + 4, 0x0E2233);

		Train train = displayedTrain.get();
		if (train != null) {

			TrainIconType icon = train.icon;
			int offset = 0;
			int position = background.width / 2 - getTrainIconWidth(train) / 2;

			List<Carriage> carriages = train.carriages;
			for (int i = carriages.size() - 1; i > 0; i--) {
				if (i == carriages.size() - 1 && train.doubleEnded) {
					offset += icon.render(TrainIconType.FLIPPED_ENGINE, ms, x + offset + position, y + 20) + 1;
					continue;
				}
				Carriage carriage = carriages.get(i);
				offset += icon.render(carriage.bogeySpacing, ms, x + offset + position, y + 20) + 1;
			}
			offset += icon.render(TrainIconType.ENGINE, ms, x + offset + position, y + 20);

			UIRenderHelper.drawStretched(ms, x + 21, y + 43, 150, 96, -100, AllGuiTextures.STATION_TEXTBOX_MIDDLE);
			AllGuiTextures.STATION_TEXTBOX_TOP.render(ms, x + 21, y + 42);
			AllGuiTextures.STATION_TEXTBOX_BOTTOM.render(ms, x + 21, y + 136);
			AllGuiTextures.STATION_TEXTBOX_SPEECH.render(ms, x + offset + position - 12, y + 38);

			TextComponent text = new TextComponent("Assembly Successful");
			font.drawShadow(ms, text, x + 97 - font.width(text) / 2, y + 47, 0xC6C6C6);
			font.drawShadow(ms,
				new TextComponent("-> " + train.carriages.size() + " Carriages, " + train.getTotalLength() + "m"),
				x + 30, y + 67, 0xC6C6C6);
			font.drawShadow(ms, new TextComponent("-> Fuel Type: NYI"), x + 30, y + 77, 0xC6C6C6);

			font.drawShadow(ms, new TextComponent("-> " + (train.doubleEnded ? "Dual Powered" : "Single Powered")),
				x + 30, y + 92, 0xC6C6C6);
			font.drawShadow(ms,
				new TextComponent((train.doubleEnded ? "(Navigates both ways)" : "(Navigates forward only)")), x + 30,
				y + 102, 0xACC4BC);
			return;
		}

		AssemblyException lastAssemblyException = te.lastException;
		if (lastAssemblyException != null) {
			TextComponent text = new TextComponent("Assembly Failed");
			font.draw(ms, text, x + 97 - font.width(text) / 2, y + 47, 0x775B5B);
			int offset = 0;
			if (te.failedCarriageIndex != -1) {
				font.draw(ms, new TextComponent("Carriage " + te.failedCarriageIndex + ":"), x + 30, y + 67, 0x7A7A7A);
				offset += 10;
			}
			font.drawWordWrap(lastAssemblyException.component, x + 30, y + 67 + offset, 134, 0x775B5B);
			offset += font.split(lastAssemblyException.component, 134)
				.size() * 9 + 5;
			font.drawWordWrap(new TextComponent("Resolve this and retry"), x + 30, y + 67 + offset, 134, 0x7A7A7A);
			return;
		}

		int bogeyCount = te.bogeyCount;

		TextComponent text =
			new TextComponent(bogeyCount == 0 ? "No Bogeys" : bogeyCount + (bogeyCount == 1 ? " Bogey" : " Bogeys"));
		font.draw(ms, text, x + 97 - font.width(text) / 2, y + 47, 0x7A7A7A);

		Component component =
			new TextComponent("Right-click on highlighted Tracks to create bogeys. Use Wrench to cycle type."
				+ "\n\nAttach structures to one or between two bogeys to form carriages.");
		font.drawWordWrap(component, x + 30, y + 67, 134, 0x7A7A7A);
	}

	@Override
	public void removed() {
		super.removed();
		Train train = displayedTrain.get();
		if (train != null) {
			ResourceLocation iconId = iconTypes.get(iconTypeScroll.getState());
			train.icon = TrainIconType.byId(iconId);
			AllPackets.channel.sendToServer(new TrainEditPacket(train.id, "", !assemblyCompleted, iconId));
		}
	}
	
	@Override
	protected PartialModel getFlag(float partialTicks) {
		return AllBlockPartials.STATION_ASSEMBLE;
	}

}
