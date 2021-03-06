package com.ponk.ddvegan.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.ponk.ddvegan.R;
import com.ponk.ddvegan.models.DataRepo;
import com.ponk.ddvegan.sync.DatabaseUpdater;

public class SplashActivity extends AppCompatActivity {

    public Animation transUp;
    public Animation transDown;
    public Animation rotate;
    public AnimationSet as;
    public ImageView icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        try {
            DataRepo.appVersionName = getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0).versionName;
            DataRepo.appVersionCode = getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        DatabaseUpdater updater = new DatabaseUpdater(this);
        updater.execute();


        icon = (ImageView)findViewById(R.id.loading_icon);
        icon.setVisibility(View.VISIBLE);
        TextView tv = (TextView)findViewById(R.id.loading_text);
        tv.setVisibility(View.VISIBLE);
        Display display = getWindowManager().getDefaultDisplay();
        double height = display.getHeight()*0.3;
        transUp = new TranslateAnimation(icon.getLeft(), icon.getLeft(), icon.getTop(), icon.getTop()-(int)height);
        transUp.setDuration(600);

        transDown = new TranslateAnimation(icon.getLeft(), icon.getLeft(), icon.getTop()-(int)height, icon.getTop());
        transDown.setDuration(600);

        rotate = new RotateAnimation(0,360, Animation.RELATIVE_TO_SELF,0.5f , Animation.RELATIVE_TO_SELF,0.5f);
        rotate.setDuration(400);

        as = new AnimationSet(true);
        as.addAnimation(rotate);
        as.addAnimation(transDown);

        transUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                icon.startAnimation(as);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        transDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                icon.startAnimation(transUp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });


        icon.startAnimation(transUp);
    }
}
