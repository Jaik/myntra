package jai.com.memorygame;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jai.com.memorygame.models.Items;
import jai.com.memorygame.models.JsonFlickrFeed;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private GridView mGridView;
    private ImageView mQuestionImageView;
    private TextView mScoreTextView;
    private String FLICKR_IMAGE_URL = "https://api.flickr.com/services/feeds/photos_public.gne?format=json";
    private List<String> mUrls;
    private int mRandomIndex = -1;
    private List<Integer> mAvailableIndexesForGame;
    private int mScorePossible;
    private int mScoreTillNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        initView();
        if (!Utility.isOnline(this)) {
            Toast.makeText(this, R.string.connect_to_network, Toast.LENGTH_LONG).show();
            return;
        }
        mAvailableIndexesForGame = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
        mScorePossible = mAvailableIndexesForGame.size() - 1;
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                if (mRandomIndex != -1 && mRandomIndex != position) {
                    Animation shakeAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                    view.startAnimation(shakeAnimation);
                    mScorePossible--;
                } else if (mRandomIndex != -1) {
                    if (mScorePossible > 0) {
                        mScoreTillNow += mScorePossible;
                    }
                    mScoreTextView.setText(String.format("Score: %d", mScoreTillNow));
                    mRandomIndex = -1;
                    ObjectAnimator animation = ObjectAnimator.ofFloat(view, "rotationY", 0.0f, 90.0f);
                    animation.setDuration(500);
                    animation.setRepeatCount(0);
                    animation.setInterpolator(new AccelerateDecelerateInterpolator());
                    animation.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Picasso.with(MainActivity.this).load(mUrls.get(position)).into(((ImageView) view));
                            ObjectAnimator animation1 = ObjectAnimator.ofFloat(view, "rotationY", 270.0f, 360.0f);
                            animation1.setDuration(500);
                            animation1.setRepeatCount(0);
                            animation1.setInterpolator(new AccelerateDecelerateInterpolator());
                            animation1.start();

                            animation1.addListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (mAvailableIndexesForGame.size() == 0) {
                                        showGameFinishDialog();
                                        return;
                                    }
                                    Random r = new Random();
                                    int result = r.nextInt(mAvailableIndexesForGame.size());
                                    result = mAvailableIndexesForGame.get(result);
                                    mScorePossible = mAvailableIndexesForGame.size() - 1;
                                    mAvailableIndexesForGame.remove((Integer) result);
                                    mRandomIndex = result;
                                    Picasso.with(MainActivity.this).load(mUrls.get(result)).into(mQuestionImageView);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                }
                            });

                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                    animation.start();
                }
            }
        });

        new FetchImagesTask().execute();
    }

    private void showGameFinishDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                        dialog.dismiss();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        finish();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);

        builder.setMessage(getString(R.string.congrats_message) + mScoreTillNow).setPositiveButton(R.string.play_again, dialogClickListener)
                .setNegativeButton(R.string.exit, dialogClickListener).show();
    }

    private void initView() {
        mGridView = (GridView) findViewById(R.id.image_grid);
        mQuestionImageView = (ImageView) findViewById(R.id.question_image);
        mScoreTextView = (TextView) findViewById(R.id.score);
    }

    class FetchImagesTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage("Loading game, please wait...");
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(List<String> urls) {
            super.onPostExecute(urls);
            mUrls = urls;
            mProgressDialog.dismiss();
            ImageGridViewAdapter adapter = new ImageGridViewAdapter(urls, MainActivity.this);
            mGridView.setAdapter(adapter);
            flipPhotoTimer.start();
        }

        public CountDownTimer flipPhotoTimer = new CountDownTimer(15000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                for (int i = 0; i < mGridView.getChildCount(); i++) {
                    final View v = mGridView.getChildAt(i);
                    ObjectAnimator animation = ObjectAnimator.ofFloat(v, "rotationY", 0.0f, 90.0f);
                    animation.setDuration(500);
                    animation.setRepeatCount(0);
                    animation.setInterpolator(new AccelerateDecelerateInterpolator());
                    final int finalI = i;
                    animation.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            ((ImageView) v).setImageResource(R.drawable.question_mark_white);
                            ObjectAnimator animation1 = ObjectAnimator.ofFloat(v, "rotationY", 270.0f, 360.0f);
                            animation1.setDuration(500);
                            animation1.setRepeatCount(0);
                            animation1.setInterpolator(new AccelerateDecelerateInterpolator());
                            animation1.start();

                            if (finalI == mGridView.getChildCount() - 1) {
                                Random r = new Random();
                                int result = r.nextInt(mAvailableIndexesForGame.size());
                                result = mAvailableIndexesForGame.get(result);
                                mScorePossible = mAvailableIndexesForGame.size() - 1;
                                mAvailableIndexesForGame.remove((Integer) result);
                                mRandomIndex = result;

                                Picasso.with(MainActivity.this).load(mUrls.get(result)).into(mQuestionImageView);
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                    animation.start();
                }
            }
        };

        @Override
        protected List<String> doInBackground(Void... params) {
            String response = HttpRequest.GET(FLICKR_IMAGE_URL);
            response = response.substring("jsonFlickrFeed(".length(), response.length() - 1);
            JsonFlickrFeed jsonFlickrFeed = new Gson().fromJson(response, JsonFlickrFeed.class);
            List<Items> items = jsonFlickrFeed.getItems();
            List<String> imageUrls = new ArrayList<>();
            for (Items item : items) {
                imageUrls.add(item.getMedia().getM());
                if (imageUrls.size() == 9) {
                    break;
                }
            }
            return imageUrls;
        }
    }
}
