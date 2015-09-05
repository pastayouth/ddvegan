package com.pasta.ddvegan.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.pasta.ddvegan.models.DataRepo;
import com.pasta.ddvegan.models.VeganNews;
import com.pasta.ddvegan.models.VeganSpot;

public class DatabaseManager extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "ddvegan.db";
	private static final int DATABASE_VERSION = 1;

	public DatabaseManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        createSpotTable(db);
        createNewsTable(db);
	}

	public void getVeganSpotsFromDatabase() {
        DataRepo.clearSpotLists();
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i("SQLite", "importing vegan spots");
		String[] projection = { "spotId", "spotName", "spotAddress", "spotPhone",
				"spotUrl", "spotMail", "spotInfo", "spotLocLong", "spotLocLat",
                "catFood", "catShopping", "catCafe", "catIcecream", "catVokue", "catBakery", "isFavorite",
                "spotHours", "spotImgKey"};
		Cursor c = db.query("veganSpots", projection, null, null, null, null, null);
		while (c.moveToNext()) {
			int spotId = c.getInt(c.getColumnIndex("spotId"));
			String spotName = c.getString(c.getColumnIndex("spotName"));
            String spotAddress = c.getString(c.getColumnIndex("spotAddress"));
            String spotPhone = c.getString(c.getColumnIndex("spotPhone"));
            String spotUrl = c.getString(c.getColumnIndex("spotUrl"));
            String spotMail = c.getString(c.getColumnIndex("spotMail"));
            String spotInfo = c.getString(c.getColumnIndex("spotInfo"));
            String spotImgKey = c.getString(c.getColumnIndex("spotImgKey"));
            String spotHours = c.getString(c.getColumnIndex("spotHours"));
            double locLong = c.getDouble(c.getColumnIndex("spotLocLong"));
            double locLat = c.getDouble(c.getColumnIndex("spotLocLat"));
            int catFood = c.getInt(c.getColumnIndex("catFood"));
            int catShopping = c.getInt(c.getColumnIndex("catShopping"));
            int catCafe = c.getInt(c.getColumnIndex("catCafe"));
            int catIcecream = c.getInt(c.getColumnIndex("catIcecream"));
            int catVokue = c.getInt(c.getColumnIndex("catVokue"));
            int catBakery = c.getInt(c.getColumnIndex("catBakery"));
            int isFavorite = c.getInt(c.getColumnIndex("isFavorite"));
			VeganSpot s = new VeganSpot(spotName, spotAddress, spotUrl, "", locLat,
            locLong, spotMail, spotInfo, spotId,  spotPhone, spotImgKey);

            s.addHours(spotHours);

            DataRepo.veganSpots.put(s.getID(),s);
            if (catFood == 1)
                DataRepo.foodSpots.add(s);
            if (catShopping == 1)
                DataRepo.shoppingSpots.add(s);
            if (catBakery == 1)
                DataRepo.bakerySpots.add(s);
            if (catIcecream == 1)
                DataRepo.icecreamSpots.add(s);
            if (catVokue == 1)
                DataRepo.vokueSpots.add(s);
            if (catCafe == 1)
                DataRepo.cafeSpots.add(s);
            if (isFavorite == 1) {
                DataRepo.favoriteMap.put(s.getID(), s);
                s.setFavorite(true);
            }
		}
		db.close();
        DataRepo.updateFavorites();
	}

    public void getVeganNewsFromDatabase() {
        SQLiteDatabase db;
        db = this.getReadableDatabase();
        DataRepo.veganNews.clear();
        Log.i("SQLite", "importing vegan news");
        String[] projection = { "newsId", "spotId", "newsType", "newsContent", "newsTime"};
        Cursor c = db.query("veganNews", projection, null, null, null, null, null);
        while (c.moveToNext()) {
            int newsId = c.getInt(c.getColumnIndex("newsId"));
            int spotId = c.getInt(c.getColumnIndex("spotId"));
            int newsType = c.getInt(c.getColumnIndex("newsType"));
            String newsTime = c.getString(c.getColumnIndex("newsTime"));
            String newsContent = c.getString(c.getColumnIndex("newsContent"));

            VeganNews n = new VeganNews(newsId, newsType, spotId, newsContent, newsTime, false);
            DataRepo.veganNews.add(n);
            Log.i("SQLITE", "getting stored news " + newsId);
        }

        if (DataRepo.veganNews.size() > 50){
            Log.i("SQLite", "deleting some old vegan news");
            int maxId = this.getMaxNewsId();
            String query = "DELETE FROM veganNews WHERE newsId < "+(maxId-50)+";";
            db = this.getWritableDatabase();
            db.execSQL(query);
        }

        db.close();
    }

    public void setAsFavorite(VeganSpot spot, boolean b) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE veganSpots SET isFavorite=";
        spot.setFavorite(b);
        if (b) {
            DataRepo.favoriteMap.put(spot.getID(), spot);
            query += "1 WHERE spotId="+spot.getID()+";";
        } else {
            DataRepo.favoriteMap.remove(spot.getID());
            query += "0 WHERE spotId="+spot.getID()+";";
        }
        db.execSQL(query);
        db.close();
        DataRepo.updateFavorites();
    }

    public boolean dbEmpty() {
        SQLiteDatabase db = this.getWritableDatabase();
        String count = "SELECT count(*) FROM veganSpots";
        Cursor c = db.rawQuery(count, null);
        if (c != null && c.moveToFirst()) {
            return (c.getInt(0) == 0);
        }
        return false;
    }

	public void createSpotTable(SQLiteDatabase db) {
		String query = "CREATE TABLE IF NOT EXISTS veganSpots ("
				+ "spotId INTEGER PRIMARY KEY, spotName TEXT, spotAddress TEXT, spotPhone TEXT," +
                "spotUrl TEXT, spotMail TEXT, spotInfo TEXT, spotLocLong REAL, spotLocLat REAL, spotImgKey TEXT," +
                "catFood INTEGER, catShopping INTEGER, catCafe INTEGER, catIcecream INTEGER, catVokue INTEGER, catBakery INTEGER," +
                "spotHours TEXT, isFavorite INTEGER);";
		db.execSQL(query);
	}

    public void createNewsTable(SQLiteDatabase db) {
        String query = "CREATE TABLE IF NOT EXISTS veganNews ("
                + "newsId INTEGER PRIMARY KEY, spotId INTEGER, newsType INTEGER, newsContent TEXT," +
                "newsTime TEXT);";
        db.execSQL(query);
    }

    public int getMaxNewsId() {
        String query = "SELECT MAX(newsId) as newsId FROM veganNews";
        SQLiteDatabase db = this.getReadableDatabase();
        int maxId = 0;
        Cursor c = db.rawQuery(query, null);
        while (c.moveToNext()) {
            maxId = c.getInt(c.getColumnIndex("newsId"));
        }
        db.close();
        return maxId;
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}