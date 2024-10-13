package run.yigou.gxzy.greendao.gen;

import java.util.List;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;

import run.yigou.gxzy.greendao.entity.YaoFangBody;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "YAO_FANG_BODY".
*/
public class YaoFangBodyDao extends AbstractDao<YaoFangBody, String> {

    public static final String TABLENAME = "YAO_FANG_BODY";

    /**
     * Properties of entity YaoFangBody.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property YaoFangBodyId = new Property(0, String.class, "yaoFangBodyId", true, "YAO_FANG_BODY_ID");
        public final static Property YaoFangID = new Property(1, String.class, "yaoFangID", false, "YAO_FANG_ID");
        public final static Property Suffix = new Property(2, String.class, "suffix", false, "SUFFIX");
        public final static Property Amount = new Property(3, String.class, "amount", false, "AMOUNT");
        public final static Property YaoID = new Property(4, int.class, "yaoID", false, "YAO_ID");
        public final static Property Weight = new Property(5, float.class, "weight", false, "WEIGHT");
        public final static Property ShowName = new Property(6, String.class, "showName", false, "SHOW_NAME");
        public final static Property ExtraProcess = new Property(7, String.class, "extraProcess", false, "EXTRA_PROCESS");
        public final static Property SignatureId = new Property(8, long.class, "signatureId", false, "SIGNATURE_ID");
        public final static Property Signature = new Property(9, String.class, "signature", false, "SIGNATURE");
    }

    private Query<YaoFangBody> yaoFang_StandardYaoListQuery;

    public YaoFangBodyDao(DaoConfig config) {
        super(config);
    }
    
    public YaoFangBodyDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"YAO_FANG_BODY\" (" + //
                "\"YAO_FANG_BODY_ID\" TEXT PRIMARY KEY NOT NULL ," + // 0: yaoFangBodyId
                "\"YAO_FANG_ID\" TEXT," + // 1: yaoFangID
                "\"SUFFIX\" TEXT," + // 2: suffix
                "\"AMOUNT\" TEXT," + // 3: amount
                "\"YAO_ID\" INTEGER NOT NULL ," + // 4: yaoID
                "\"WEIGHT\" REAL NOT NULL ," + // 5: weight
                "\"SHOW_NAME\" TEXT," + // 6: showName
                "\"EXTRA_PROCESS\" TEXT," + // 7: extraProcess
                "\"SIGNATURE_ID\" INTEGER NOT NULL ," + // 8: signatureId
                "\"SIGNATURE\" TEXT);"); // 9: signature
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"YAO_FANG_BODY\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, YaoFangBody entity) {
        stmt.clearBindings();
 
        String yaoFangBodyId = entity.getYaoFangBodyId();
        if (yaoFangBodyId != null) {
            stmt.bindString(1, yaoFangBodyId);
        }
 
        String yaoFangID = entity.getYaoFangID();
        if (yaoFangID != null) {
            stmt.bindString(2, yaoFangID);
        }
 
        String suffix = entity.getSuffix();
        if (suffix != null) {
            stmt.bindString(3, suffix);
        }
 
        String amount = entity.getAmount();
        if (amount != null) {
            stmt.bindString(4, amount);
        }
        stmt.bindLong(5, entity.getYaoID());
        stmt.bindDouble(6, entity.getWeight());
 
        String showName = entity.getShowName();
        if (showName != null) {
            stmt.bindString(7, showName);
        }
 
        String extraProcess = entity.getExtraProcess();
        if (extraProcess != null) {
            stmt.bindString(8, extraProcess);
        }
        stmt.bindLong(9, entity.getSignatureId());
 
        String signature = entity.getSignature();
        if (signature != null) {
            stmt.bindString(10, signature);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, YaoFangBody entity) {
        stmt.clearBindings();
 
        String yaoFangBodyId = entity.getYaoFangBodyId();
        if (yaoFangBodyId != null) {
            stmt.bindString(1, yaoFangBodyId);
        }
 
        String yaoFangID = entity.getYaoFangID();
        if (yaoFangID != null) {
            stmt.bindString(2, yaoFangID);
        }
 
        String suffix = entity.getSuffix();
        if (suffix != null) {
            stmt.bindString(3, suffix);
        }
 
        String amount = entity.getAmount();
        if (amount != null) {
            stmt.bindString(4, amount);
        }
        stmt.bindLong(5, entity.getYaoID());
        stmt.bindDouble(6, entity.getWeight());
 
        String showName = entity.getShowName();
        if (showName != null) {
            stmt.bindString(7, showName);
        }
 
        String extraProcess = entity.getExtraProcess();
        if (extraProcess != null) {
            stmt.bindString(8, extraProcess);
        }
        stmt.bindLong(9, entity.getSignatureId());
 
        String signature = entity.getSignature();
        if (signature != null) {
            stmt.bindString(10, signature);
        }
    }

    @Override
    public String readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0);
    }    

    @Override
    public YaoFangBody readEntity(Cursor cursor, int offset) {
        YaoFangBody entity = new YaoFangBody( //
            cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0), // yaoFangBodyId
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // yaoFangID
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // suffix
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // amount
            cursor.getInt(offset + 4), // yaoID
            cursor.getFloat(offset + 5), // weight
            cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6), // showName
            cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7), // extraProcess
            cursor.getLong(offset + 8), // signatureId
            cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9) // signature
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, YaoFangBody entity, int offset) {
        entity.setYaoFangBodyId(cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0));
        entity.setYaoFangID(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setSuffix(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setAmount(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setYaoID(cursor.getInt(offset + 4));
        entity.setWeight(cursor.getFloat(offset + 5));
        entity.setShowName(cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6));
        entity.setExtraProcess(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
        entity.setSignatureId(cursor.getLong(offset + 8));
        entity.setSignature(cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9));
     }
    
    @Override
    protected final String updateKeyAfterInsert(YaoFangBody entity, long rowId) {
        return entity.getYaoFangBodyId();
    }
    
    @Override
    public String getKey(YaoFangBody entity) {
        if(entity != null) {
            return entity.getYaoFangBodyId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(YaoFangBody entity) {
        return entity.getYaoFangBodyId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
    /** Internal query to resolve the "standardYaoList" to-many relationship of YaoFang. */
    public List<YaoFangBody> _queryYaoFang_StandardYaoList(String yaoFangID) {
        synchronized (this) {
            if (yaoFang_StandardYaoListQuery == null) {
                QueryBuilder<YaoFangBody> queryBuilder = queryBuilder();
                queryBuilder.where(Properties.YaoFangID.eq(null));
                yaoFang_StandardYaoListQuery = queryBuilder.build();
            }
        }
        Query<YaoFangBody> query = yaoFang_StandardYaoListQuery.forCurrentThread();
        query.setParameter(0, yaoFangID);
        return query.list();
    }

}
