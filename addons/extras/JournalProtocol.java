package holylois.boombox;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Fabric dependencies do not order main entrypoints. Either add-on can prepare the shared journal protocol first. */
public final class JournalProtocol {
    private static boolean registered;
    private JournalProtocol() {}

    public static synchronized void register(){
        if(registered)return;
        PayloadTypeRegistry.clientboundPlay().register(JournalState.TYPE,JournalState.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(JournalUpdate.TYPE,JournalUpdate.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(JournalRequest.TYPE,JournalRequest.CODEC);
        registered=true;
    }
}
