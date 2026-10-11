package holylois.boombox.mixins;

import holylois.boombox.ShaderPacks;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.discovery.ShaderpackDirectoryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shader list light to heavy, and only the Euphoria Patches builds of Complementary (owner 2026-10-11). The plain zips stay on disk because
 * Euphoria Patcher builds its "+ EuphoriaPatches" folders from them; a player who still runs a plain one keeps seeing it.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gui.element.ShaderPackSelectionList", remap = false)
public abstract class ShaderPackListMixin {
    @Redirect(method = "refresh", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/shaderpack/discovery/ShaderpackDirectoryManager;enumerate()Ljava/util/List;"), require = 0)
    private List<String> holyLoisHidePlainComplementary(ShaderpackDirectoryManager manager) throws IOException {
        List<String> packs = new ArrayList<>(manager.enumerate());
        String applied = Iris.getIrisConfig().getShaderPackName().orElse("");
        packs.removeIf(name -> name.startsWith("Complementary") && name.endsWith(".zip") && !name.equals(applied)
            && packs.stream().anyMatch(other -> other.startsWith(name.substring(0, name.length() - 4) + " + EuphoriaPatches")));
        packs.sort(ShaderPacks.ORDER);
        return packs;
    }
}
