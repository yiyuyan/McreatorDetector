package cn.ksmcbrigade.MCRD;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

@Mod("mcrd")
@Mod.EventBusSubscriber
public class McreatorDetector {

    private static final Logger LOGGER = LogManager.getLogger();
    private static ArrayList<IModInfo> McreatorMods = new ArrayList<>();
    private static ArrayList<String> McreatorModsDetails = new ArrayList<>();

    public McreatorDetector() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void detectCommand(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("detect").executes((context -> {
            Entity player = context.getSource().getEntity();
            if (player != null) {
                McreatorMods = new ArrayList<>();
                McreatorModsDetails = new ArrayList<>();
                LOGGER.info("Detecting.");
                player.sendMessage(new TranslatableComponent("commands.mcrd.detecting"),player.getUUID());
                for(IModInfo mod: ModList.get().getMods()){
                    String strings = GetString(mod);
                    String packets = getPackets(mod.getOwningFile().getFile().getSecureJar().getPackages());
                    if(strings.toLowerCase().contains("mcreator") && !mod.getModId().equals("mcrd")){
                        McreatorMods.add(mod);
                        McreatorModsDetails.add(strings);
                    }
                    else if((packets.toLowerCase().contains("mcreator") && !mod.getModId().equals("mcrd")) || (packets.toLowerCase().contains("procedures") && !mod.getModId().equals("mcrd"))){
                        McreatorMods.add(mod);
                        McreatorModsDetails.add(packets);
                    }
                    else if(mod.getOwningFile().getFile().getFilePath().toString().toLowerCase().contains("mcreator")){
                        McreatorMods.add(mod);
                        McreatorModsDetails.add("Found \"mcreator\" in the mod file path string.");
                    }
                }
                LOGGER.info(I18n.get("message.mcrd.find").replace("{i}",String.valueOf(McreatorMods.size())));
                if(!McreatorMods.equals(new ArrayList<>())){
                    StringBuilder message;
                    StringBuilder stringBuilder = new StringBuilder(I18n.get("message.mcrd.find").replace("{i}", String.valueOf(McreatorMods.size())) +"\n");
                    stringBuilder.append(I18n.get("message.mcrd.name").replace("{names}", getNamesOrIds(McreatorMods, false))).append("\n");
                    stringBuilder.append(I18n.get("message.mcrd.id").replace("{ids}", getNamesOrIds(McreatorMods, true))).append("\n");
                    message = new StringBuilder(stringBuilder.toString());
                    message.append(I18n.get("message.mcrd.details"));
                    stringBuilder.append(new TranslatableComponent("file.mcrd.details").getString()).append("\n");
                    for(int i=0;i<McreatorMods.size();i++) {
                        IModInfo modInfo = McreatorMods.get(i);
                        String details = McreatorModsDetails.get(i);
                        stringBuilder.append(modInfo.getDisplayName()).append("(").append(modInfo.getModId()).append("): ").append(details).append("\n");
                    }
                    try {
                        Files.write(Paths.get("output.txt"),stringBuilder.toString().getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    for(String string:message.toString().split("\n")){
                        player.sendMessage(Component.nullToEmpty(string),player.getUUID());
                    }
                    player.sendMessage(Component.nullToEmpty(""),player.getUUID());
                    player.sendMessage(new TranslatableComponent("commands.mcrd.done"),player.getUUID());
                }
            }
            return 0;
        })));
    }

    public static String GetString(IModInfo mod){
        JsonObject json = new JsonObject();
        json.addProperty("name",mod.getDisplayName());
        json.addProperty("id",mod.getModId());
        json.addProperty("namespace",mod.getNamespace());
        json.addProperty("updateURL",mod.getUpdateURL().toString());
        json.addProperty("version",mod.getVersion().toString());
        json.addProperty("describe",mod.getDescription());
        json.addProperty("credits",mod.getConfig().getConfigElement("credits").toString());
        json.addProperty("authors",mod.getConfig().getConfigElement("authors").toString());
        json.addProperty("displayURL",mod.getConfig().getConfigElement("displayURL").toString());
        json.addProperty("license",mod.getOwningFile().getLicense());
        return json.toString();
    }

    public static String getNamesOrIds(ArrayList<IModInfo> mods,boolean ids){
        StringBuilder ret = new StringBuilder();
        for(IModInfo mod:mods){
            if(ids){
                if(ret.isEmpty()){
                    ret.append(mod.getModId());
                }
                else{
                    ret.append(", ");
                    ret.append(mod.getModId());
                }
            }
            else{
                if(ret.isEmpty()){
                    ret.append(mod.getDisplayName());
                }
                else{
                    ret.append(", ");
                    ret.append(mod.getDisplayName());
                }
            }
        }
        return ret.toString();
    }

    /*public static String getAnnotationDatum(Set<ModFileScanData.AnnotationData> data){
        JsonObject json = new JsonObject();
        for(ModFileScanData.AnnotationData datum:data){
            if(!datum.clazz().getClassName().contains("net.minecraft")){
                json.addProperty(datum.memberName(),datum.clazz().getClassName());
            }
        }
        return json.toString();
    }*/

    public static String getPackets(Set<String> data){
        JsonObject json = new JsonObject();
        for(String datum:data){
            if(!datum.contains("net.minecraft")){
                json.add(datum,null);
            }
        }
        return json.toString();
    }
}
