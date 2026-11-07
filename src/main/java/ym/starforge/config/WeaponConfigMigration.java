package ym.starforge.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;

/** Schema 1 selectors remain valid in schema 2; preserve every comment and unknown field. Async only. */
public final class WeaponConfigMigration {
    public static void migrate(Path file)throws IOException{
        String source=Files.readString(file,StandardCharsets.UTF_8);
        var pattern=java.util.regex.Pattern.compile("(?m)^(\\uFEFF?schema-version:[ \\t]*)1([ \\t]*(?:#.*)?\\r?)$");
        var matcher=pattern.matcher(source);if(!matcher.find())return;
        String replacement=source.substring(0,matcher.start())+matcher.group(1)+"2"+matcher.group(2)+source.substring(matcher.end());
        Path backup=file.resolveSibling(file.getFileName()+".v1.bak");
        if(!Files.exists(backup))Files.copy(file,backup);
        Path temp=Files.createTempFile(file.getParent(),"weapons-migration-",".tmp");
        try{
            Files.writeString(temp,replacement,StandardCharsets.UTF_8);
            if(!Files.readString(file,StandardCharsets.UTF_8).equals(source))throw new IOException("file changed while migrating");
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException ex){throw new IOException("weapons.yml:schema-version 1 -> 2 migration failed; original/backup retained: "+file+": "+ex.getMessage(),ex);}
        finally{Files.deleteIfExists(temp);}
    }
}
