package run.yigou.gxzy.greendao.gen;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import run.yigou.gxzy.greendao.entity.ZhongYaoAlia;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "YAO_ALIA".
*/
public class YaoAliaDao extends AbstractDao<ZhongYaoAlia, Long> {

    public static final String TABLENAME = "YAO_ALIA";

    /**
     * Properties of entity YaoAlia.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property YaoAliaId = new Property(0, Long.class, "yaoAliaId", true, "_id");
        public final static Property Id = new Property(1, int.class, "id", false, "ID");
        public final static Property Name = new Property(2, String.class, "name", false, "NAME");
        public final static Property Bieming = new Property(3, String.class, "bieming", false, "BIEMING");
        public final static Property Height = new Property(4, int.class, "height", false, "HEIGHT");
        public final static Property Text = new Property(5, String.class, "text", false, "TEXT");
        public final static Property SignatureId = new Property(6, long.class, "signatureId", false, "SIGNATURE_ID");
        public final static Property Signature = new Property(7, String.class, "signature", false, "SIGNATURE");
    }


    public YaoAliaDao(DaoConfig config) {
        super(config);
    }
    
    public YaoAliaDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"YAO_ALIA\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: yaoAliaId
                "\"ID\" INTEGER NOT NULL ," + // 1: id
                "\"NAME\" TEXT," + // 2: name
                "\"BIEMING\" TEXT," + // 3: bieming
                "\"HEIGHT\" INTEGER NOT NULL ," + // 4: height
                "\"TEXT\" TEXT," + // 5: text
                "\"SIGNATURE_ID\" INTEGER NOT NULL ," + // 6: signatureId
                "\"SIGNATURE\" TEXT);"); // 7: signature
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"YAO_ALIA\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, ZhongYaoAlia entity) {
        stmt.clearBindings();
 
        Long yaoAliaId = entity.getYaoAliaId();
        if (yaoAliaId != null) {
            stmt.bindLong(1, yaoAliaId);
        }
        stmt.bindLong(2, entity.getId());
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
 
        String bieming = entity.getBieming();
        if (bieming != null) {
            stmt.bindString(4, bieming);
        }
        stmt.bindLong(5, entity.getHeight());
 
        String text = entity.getText();
        if (text != null) {
            stmt.bindString(6, text);
        }
        stmt.bindLong(7, entity.getSignatureId());
 
        String signature = entity.getSignature();
        if (signature != null) {
            stmt.bindString(8, signature);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, ZhongYaoAlia entity) {
        stmt.clearBindings();
 
        Long yaoAliaId = entity.getYaoAliaId();
        if (yaoAliaId != null) {
            stmt.bindLong(1, yaoAliaId);
        }
        stmt.bindLong(2, entity.getId());
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
 
        String bieming = entity.getBieming();
        if (bieming != null) {
            stmt.bindString(4, bieming);
        }
        stmt.bindLong(5, entity.getHeight());
 
        String text = entity.getText();
        if (text != null) {
            stmt.bindString(6, text);
        }
        stmt.bindLong(7, entity.getSignatureId());
 
        String signature = entity.getSignature();
        if (signature != null) {
            stmt.bindString(8, signature);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public ZhongYaoAlia readEntity(Cursor cursor, int offset) {
        ZhongYaoAlia entity = new ZhongYaoAlia( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // yaoAliaId
            cursor.getInt(offset + 1), // id
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // name
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // bieming
            cursor.getInt(offset + 4), // height
            cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5), // text
            cursor.getLong(offset + 6), // signatureId
            cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7) // signature
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, ZhongYaoAlia entity, int offset) {
        entity.setYaoAliaId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setId(cursor.getInt(offset + 1));
        entity.setName(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setBieming(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setHeight(cursor.getInt(offset + 4));
        entity.setText(cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5));
        entity.setSignatureId(cursor.getLong(offset + 6));
        entity.setSignature(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(ZhongYaoAlia entity, long rowId) {
        entity.setYaoAliaId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(ZhongYaoAlia entity) {
        if(entity != null) {
            return entity.getYaoAliaId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(ZhongYaoAlia entity) {
        return entity.getYaoAliaId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
