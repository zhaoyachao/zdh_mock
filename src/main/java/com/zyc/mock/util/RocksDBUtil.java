package com.zyc.mock.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBUtil {

    static {
        RocksDB.loadLibrary();
    }

    public static Cache<String, RocksDB> rocksDBCache = CacheBuilder.newBuilder().build();

    public static RocksDB getConnection(String path) throws RocksDBException {
        synchronized (path.intern()){
            if(rocksDBCache.getIfPresent(path) != null){
                return rocksDBCache.getIfPresent(path);
            }
            synchronized ("lock".intern()){
                Options options = new Options().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
                RocksDB rocksDB = RocksDB.open(options, path);
                rocksDBCache.put(path, rocksDB);
                return rocksDBCache.getIfPresent(path);
            }
        }
    }

    public static RocksDB getReadOnlyConnection(String path) throws RocksDBException {
        Options options = new Options().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
        RocksDB rocksDB = RocksDB.openReadOnly(path);
        return rocksDB;
    }

    public static void put(String path, String key, String value){
        try{
            RocksDB rocksDB = getConnection(path);
            rocksDB.put(key.getBytes(), value.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static String get(String path, String key){
        try{
            RocksDB rocksDB = getReadOnlyConnection(path);
            byte[] value = rocksDB.get(key.getBytes());
            return new String(value);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }


    public static Long getIncr(String path, String key){
        try{
            synchronized (path.intern()){
                RocksDB rocksDB = getConnection(path);
                byte[] value = rocksDB.get(key.getBytes());
                Long incr = 1L;
                if(value !=  null && new String(value) != ""){
                    incr = Long.parseLong(new String(value))  + 1;
                }
                rocksDB.put(key.getBytes(), String.valueOf(incr).getBytes());
                return incr;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1L;
    }
}
