package com.birbit.example.secondarydex.big.base;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import dalvik.system.DexClassLoader;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * this class handles loading Joda library from secondary dex file
 * it checks when the last time dex file was created to decide if it should re-copy it or
 * use existing copy. this way, it will remember to re-run in your next build but avoid
 * running again and again in each app start
 */
public class JodaLoader {
    private static final String DATE_PARSER_CLASSNAME = "com.birbit.example.secondarydex.big.stuff.DateParser";
    private static final String DEX_ASSET_NAME = "secondary_dex.jar";
    private static final String SECONDARY_DEX_PREFIX = "secondary_dex_";
    private static final String SECONDARY_DEX_SUFFIX = ".jar";
    private static final String TAG = "JodaLoader";
    private static final int BUF_SIZE = 8 * 1024;
    private static final String OUT_DEX_DIR_PREFIX = "outdex_";
    File dexInternalStoragePath;
    DexClassLoader classLoader;
    Context appContext;
    private Long cachedAppBuildTime;

    public JodaLoader(Context context) {
        appContext = context.getApplicationContext();
        dexInternalStoragePath = new File(appContext.getDir("dex", Context.MODE_PRIVATE),
                getSecondaryDexFileName());
        log("dex internal storage path: " + dexInternalStoragePath.getAbsolutePath());
        String secondaryDexFileName = getSecondaryDexFileName();
        //this parses every single time
        //should be once per install!!!
        if(new File(secondaryDexFileName).exists() == false || isLocked(getSecondaryDexFileName()) == false) {
            if(prepareDex()) {
                log("created dex jar");
                addCreatedLock(secondaryDexFileName);
            } else {
                cleanCreatedLock(getSecondaryDexFileName());
                throw new RuntimeException("could not prepare dex file!!!");
            }
        } else {
            log("secondary dex file is already created, good");
        }
        createClassLoader();
    }

    private void createClassLoader() {
        log("creating dex class loader");
        // Internal storage where the DexClassLoader writes the optimized dex file to.
        File optimizedDexFolder = appContext.getDir("outdex", Context.MODE_PRIVATE);
        final File optimizedDexOutputPath = new File(optimizedDexFolder, getOutDexDir());
        cleanPreviousOptimizedDexFolders(optimizedDexFolder, getOutDexDir());
        //noinspection ResultOfMethodCallIgnored
        optimizedDexOutputPath.mkdir();
        log("optimized output dir " + optimizedDexOutputPath.getAbsolutePath());
        // Initialize the class loader with the secondary dex file.
        classLoader = new DexClassLoader(dexInternalStoragePath.getAbsolutePath(),
                optimizedDexOutputPath.getAbsolutePath(),
                null,
                appContext.getClassLoader());
    }

    private void log(String str) {
        Log.d(TAG, str);
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    public IDateParser loadDateParser() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return (IDateParser) loadClass(DATE_PARSER_CLASSNAME).newInstance();
    }

    // we use these lock files to mark these folders as written successfully

    private File toLockFile(String filename) {
        return new File(appContext.getDir("locks", Context.MODE_PRIVATE), "lock_" + filename);
    }

    private void addCreatedLock(String filename) {
        log("locking file " + filename);
        File file = toLockFile(filename);
        try {
            file.createNewFile();
        } catch (IOException e) {

        }
    }
    private boolean isLocked(String filename) {
        return toLockFile(filename).exists();
    }

    private void cleanCreatedLock(String filename) {
        log("cleaning lock for file " + filename);
        //noinspection ResultOfMethodCallIgnored
        toLockFile(filename).delete();
    }

    // File I/O code to copy the secondary dex file from asset resource to internal storage.
    private boolean prepareDex() {
        log("prepare dex");
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;
        String targetFilename = getSecondaryDexFileName();
        log("target filename " + targetFilename);
        cleanPreviousDexFiles(targetFilename);
        try {
            bis = new BufferedInputStream(appContext.getAssets().open(DEX_ASSET_NAME));
            dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();
            return true;
        } catch (IOException e) {
            if (dexWriter != null) {
                try {
                    dexWriter.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            return false;
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        //noinspection ResultOfMethodCallIgnored
        fileOrDirectory.delete();
    }

    private void cleanPreviousOptimizedDexFolders(File outDexFolder, String exclude) {
        File[] files = outDexFolder.listFiles();
        if(files == null) {
            return;
        }
        for(File file : files) {
            if(file.isDirectory()
                    && exclude.equals(file.getName()) == false
                    && file.getName().startsWith(OUT_DEX_DIR_PREFIX)) {
                deleteRecursive(file);
                log("cleaned dex output folder " + file.getName());
            }
        }
    }

    private void cleanPreviousDexFiles(String exclude) {
        File dexDir = appContext.getDir("dex", Context.MODE_PRIVATE);
        if(dexDir.isDirectory()) {
            //we are not using filters here, we got some logs that they may cause problems
            File[] files = dexDir.listFiles();
            if(files == null) {
                return;
            }
            for(File file : files) {
                if( file.isFile()
                        && exclude.equals(file.getName()) == false
                        && file.getName().startsWith(SECONDARY_DEX_PREFIX)
                        && file.getName().endsWith(SECONDARY_DEX_SUFFIX)) {
                    cleanCreatedLock(file.getName());
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    log("cleaned dex file " + file.getName() );

                }
            }
        }
    }

    private String getOutDexDir() {
        return OUT_DEX_DIR_PREFIX + getAppBuildTime();
    }

    private String getSecondaryDexFileName() {
        return SECONDARY_DEX_PREFIX + getAppBuildTime() + SECONDARY_DEX_SUFFIX;
    }

    private long getAppBuildTime() {
        if(cachedAppBuildTime == null) {
            try{
                ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), 0);
                ZipFile zf = new ZipFile(ai.sourceDir);
                ZipEntry ze = zf.getEntry("classes.dex");
                cachedAppBuildTime = ze.getTime();
                log("app build time " + cachedAppBuildTime);
            }catch(Throwable t){
                return 1;
            }
        }

        return cachedAppBuildTime;
    }
}
