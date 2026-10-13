package holylois.gametest;

import holylois.boombox.BundledPackOrder;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.resources.Identifier;
import java.util.*;

final class ManagedPackChecks {
    static void run(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        context.runOnClient(client -> {
            var repo=client.getResourcePackRepository();
            checkOrder(repo);
            for (String id : BundledPackOrder.IDS) {
                var pack=repo.getPack(id);
                require(pack.isRequired() && pack.isFixedPosition(), "Bundled pack is mutable: " + id);
                require(Boolean.TRUE.equals(fuzs.resourcepackoverrides.common.config.ResourceOverridesManager.getOverride(id).hidden()), "Bundled pack is visible: " + id);
            }
            var reversed=new ArrayList<>(repo.getSelectedIds());Collections.reverse(reversed);repo.setSelected(reversed);
            checkOrder(repo);
            repo.setSelected(List.of("vanilla"));checkOrder(repo);
            client.options.updateResourcePacks(repo);
            // Fixed packs are intentionally absent from options after save. A subsequent reload must restore their order.
            repo.reload();client.options.loadSelectedResourcePacks(repo);checkOrder(repo);
            client.gui.setScreen(new net.minecraft.client.gui.screens.packs.PackSelectionScreen(repo, ignored -> client.gui.setScreen(null),client.gameDirectory.toPath().resolve("resourcepacks"),net.minecraft.network.chat.Component.literal("Resource packs")));
        });
        context.waitFor(client -> client.gui.overlay()==null,400);
        context.waitTicks(5);context.takeScreenshot("managed-packs-hidden");
        checkFolder(context);
        context.runOnClient(client -> client.gui.setScreen(null));
        var folder=context.computeOnClient(client -> client.gameDirectory.toPath().resolve("resourcepacks"));
        byte[] metadata;
        try(var original=new java.util.zip.ZipFile(folder.resolve("HolyLois-FishEating.zip").toFile())) {
            metadata=original.getInputStream(original.getEntry("pack.mcmeta")).readAllBytes();
        }
        for (String name : List.of("Player Textures.zip","Player Sounds.zip")) {
            try(var zip=new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(folder.resolve(name)))) {
                zip.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));zip.write(metadata);zip.closeEntry();
            }
        }
        context.runOnClient(client -> {
            var repo=client.getResourcePackRepository();repo.reload();
            var selected=new ArrayList<>(repo.getSelectedIds());selected.add("file/Player Textures.zip");selected.add("file/Player Sounds.zip");repo.setSelected(selected);
            checkOrder(repo);
            for (String id : List.of("file/Player Textures.zip","file/Player Sounds.zip"))
                require(repo.getPack(id)!=null && !repo.getPack(id).isRequired() && !repo.getPack(id).isFixedPosition(), "Player pack was locked: " + id);
            selected=new ArrayList<>(repo.getSelectedIds());Collections.swap(selected,selected.indexOf("file/Player Textures.zip"),selected.indexOf("file/Player Sounds.zip"));repo.setSelected(selected);
            require(new ArrayList<>(repo.getSelectedIds()).indexOf("file/Player Sounds.zip")<new ArrayList<>(repo.getSelectedIds()).indexOf("file/Player Textures.zip"),"Personal pack order was not retained");
            checkOrder(repo);client.options.updateResourcePacks(repo);
            repo.reload();client.options.loadSelectedResourcePacks(repo);checkOrder(repo);
            require(repo.getSelectedIds().containsAll(List.of("file/Player Textures.zip","file/Player Sounds.zip")),"Personal selection lost on reload");
            client.gui.setScreen(new net.minecraft.client.gui.screens.packs.PackSelectionScreen(repo, ignored -> client.gui.setScreen(null),folder,net.minecraft.network.chat.Component.literal("Resource packs")));
        });
        context.waitFor(client -> client.gui.overlay()==null,400);
        context.waitTicks(5);context.takeScreenshot("managed-packs-personal-overlays");
        context.runOnClient(client -> client.gui.setScreen(null));
        var reloaded=context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> reloaded.isDone(),400);
        context.runOnClient(client -> {
            require(client.getResourceManager().getResource(Identifier.parse("minecraft:emf/cem/player.jem")).orElseThrow().sourcePackId().equals("file/FA+Player-v1.1.zip"),"Player animations changed after locked-pack reload");
            org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info("Managed packs hidden/fixed, required order restored, personal packs/reload and player resource verified");
        });
    }
    private static holylois.boombox.ManagedPackMenu.GroupEntry folder(net.minecraft.client.gui.screens.Screen screen) {
        var groups=screen.children().stream().filter(net.minecraft.client.gui.screens.packs.TransferableSelectionList.class::isInstance)
            .map(net.minecraft.client.gui.screens.packs.TransferableSelectionList.class::cast)
            .flatMap(list->list.children().stream()).filter(holylois.boombox.ManagedPackMenu.GroupEntry.class::isInstance).toList();
        require(groups.size()==1,"Expected one read-only managed folder, got "+groups.size());
        return (holylois.boombox.ManagedPackMenu.GroupEntry)groups.getFirst();
    }
    private static void checkFolder(ClientGameTestContext context) {
        var parent=context.computeOnClient(client->client.gui.screen());
        var selected=context.computeOnClient(client->List.copyOf(client.getResourcePackRepository().getSelectedIds()));
        var point=context.computeOnClient(client->{
            var search=(net.minecraft.client.gui.components.EditBox)parent.children().stream()
                .filter(net.minecraft.client.gui.components.EditBox.class::isInstance).findFirst().orElseThrow();
            search.setValue("Fresh Animations");folder(parent);
            search.setValue("zz-holylois-no-matching-pack");
            require(parent.children().stream().filter(net.minecraft.client.gui.screens.packs.TransferableSelectionList.class::isInstance)
                .map(net.minecraft.client.gui.screens.packs.TransferableSelectionList.class::cast)
                .flatMap(list->list.children().stream()).noneMatch(entry->holylois.boombox.ManagedPackMenu.ID.equals(entry.getPackId())),"Folder ignored search");
            search.setValue("");
            var group=folder(parent);double scale=client.getWindow().getGuiScale();
            require(client.getResourcePackRepository().getPack(holylois.boombox.ManagedPackMenu.ID)==null,"Folder became a real pack");
            return new double[]{(group.getContentX()+45)*scale,(group.getContentY()+8)*scale};
        });
        context.getInput().setCursorPos(point[0],point[1]);
        context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        context.runOnClient(client->{
            require(client.gui.screen() instanceof holylois.boombox.ManagedResourcesScreen,"Folder click did not open contents");
            var contents=(holylois.boombox.ManagedResourcesScreen)client.gui.screen();
            require(contents.packIds().equals(holylois.boombox.BundledPackOrder.IDS.reversed()),"Folder priority differs from actual managed order");
            require(contents.children().size()==1,"Read-only contents gained pack controls");
            require(List.copyOf(client.getResourcePackRepository().getSelectedIds()).equals(selected),"Viewing contents changed selection");
            int width=contents.width,height=contents.height;
            contents.resize(640,480);
            require(!contents.mouseScrolled(320,240,0,-1),"Contents scroll despite fitting in the panel");
            contents.resize(width,height);
        });
        context.takeScreenshot("managed-folder-contents");
        context.runOnClient(client->client.gui.screen().mouseScrolled(client.gui.screen().width/2.,client.gui.screen().height/2.,0,-100));
        context.waitTicks(3);context.takeScreenshot("managed-folder-contents-bottom");
        context.clickScreenButton("holylois.resources.back");context.waitTicks(3);
        context.runOnClient(client->{
            require(client.gui.screen()==parent,"Back lost the original pack screen");
            var list=parent.children().stream().filter(net.minecraft.client.gui.screens.packs.TransferableSelectionList.class::isInstance)
                .map(net.minecraft.client.gui.screens.packs.TransferableSelectionList.class::cast)
                .filter(value->value.children().stream().anyMatch(entry->entry.getPackId().equals(holylois.boombox.ManagedPackMenu.ID))).findFirst().orElseThrow();
            list.setSelected(folder(parent));list.setFocused(folder(parent));parent.setFocused(list);
        });
        context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN);context.waitTicks(3);
        context.runOnClient(client->require(client.gui.screen() instanceof holylois.boombox.ManagedResourcesScreen,"Keyboard could not open managed contents"));
        context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);context.waitTicks(3);
        context.runOnClient(client->{
            require(client.gui.screen()==parent,"Escape lost the original pack screen");
            require(List.copyOf(client.getResourcePackRepository().getSelectedIds()).equals(selected),"Folder navigation changed selected packs");
        });
    }
    private static void checkOrder(net.minecraft.server.packs.repository.PackRepository repo) {
        var selected=new ArrayList<>(repo.getSelectedIds());
        require(selected.stream().filter(BundledPackOrder.IDS::contains).toList().equals(BundledPackOrder.IDS),"Managed order changed: "+selected);
        int start=selected.indexOf(BundledPackOrder.IDS.getFirst());
        require(selected.subList(start,start+BundledPackOrder.IDS.size()).equals(BundledPackOrder.IDS),"Managed block is not contiguous");
        for(int i=start+BundledPackOrder.IDS.size();i<selected.size();i++){
            var pack=repo.getPack(selected.get(i));
            require(!pack.isRequired() || pack.getPackSource()==net.minecraft.server.packs.repository.PackSource.SERVER || pack.getPackSource()==net.minecraft.server.packs.repository.PackSource.WORLD,"Mod assets override managed animations: "+pack.getId());
        }
    }
    private static void require(boolean result,String message) { if(!result)throw new AssertionError(message); }
}
