package net.raj;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class LetItSnow extends Activity {
  
    static final int MENU_NEW = 0;
    static final int MENU_PIC = 1;
    static final int MENU_ABOUT = 2;
    static final int MENU_QUIT = 3;
   
    static final int REQUEST_FILEPICK = 1;
   
    static final int DIALOG_ABOUT = 1;
   
    static final int spread = 0;
    static final int nSegments = 6;
    static final float lenRatio = 0.6f;
    static final int angularDelta = 125;
    static final int MAX_LANDSCAPES = 3;
   
    Rect currentSize;
   
    private final Paint mPaint = new Paint();
    SnowView snowView;
   
    ArrayList<Particle> particles;
    float ctr[];
   
    Bitmap buffer;
   
    boolean inited;
    boolean snowHitGround;
    int generateNew;
   
    String imageFilePath="";
    SnowThread snowThread;
   
    private ProgressDialog pd;
  
    private void drawFractal(Canvas c, Paint p, int x, int y, int r, double theta, int level) {
        if (level == 0) return;
      
        Random rnd = new Random();          
        if (spread > 0) {
            theta += (rnd.nextInt(spread) - spread/2);
        }
      
        int x1 = x + (int) (r*Math.cos(Math.toRadians(theta)));
        int y1 = y - (int) (r*Math.sin(Math.toRadians(theta)));

        c.drawLine(x, y, x1, y1, p);
      
        for (int i=0; i<nSegments; i++) {
            double r1 = (1.0*(nSegments-i)/nSegments)*r;
            double l1 = (1.0*(i+1)/nSegments)*r;
            //double theta1 = theta + (rnd.nextInt(4)-2);
            x1 = x + (int) (r1*Math.cos(Math.toRadians(theta)));
            y1 = y - (int) (r1*Math.sin(Math.toRadians(theta)));
            drawFractal(c, p, x1, y1, (int) (l1*lenRatio), theta - angularDelta, level-1);
            drawFractal(c, p, x1, y1, (int) (l1*lenRatio), theta + angularDelta, level-1);              
          
        }
    }  
   
    private void drawTree(Canvas c, Paint p, int x, int y, int r, double theta, int level) {
        Path path = new Path();
        path.moveTo(x-(int)(r*0.2), y+(int) (r*0.3));
        path.lineTo(x, y-r);
        path.lineTo(x+(int)(r*0.2), y+(int) (r*0.3));
        path.close();
       
        p.setColor(0xff8b4513);
        c.drawPath(path, p);
       
        p.setARGB(255, 0, 255, 0);
        drawFractal(c, p, x, y, r, theta, level);
    }
   
    private void drawLandscape0(Canvas c, Rect bounds) {
        Paint p = new Paint();
        p.setARGB(255, 255, 255, 255);
       
        int maxX = (int) (bounds.right * 0.6);
        double conv = 1.5 * Math.PI / maxX;
        for (int i=0; i<maxX; i++) {
            int height = (int) (30*Math.sin(i*conv) + 60);
            c.drawLine(i, bounds.bottom, i, bounds.bottom - height, p);
        }
       
        for (int i=maxX; i<=bounds.right; i++) {
            c.drawLine(i, bounds.bottom, i, bounds.bottom - 30, p);           
        }
       
        drawTree(c, p, (int) (0.8*bounds.right), bounds.bottom - 50, 70, 90, 4);
        drawTree(c, p, maxX/2 - 45, bounds.bottom - 110, 120, 90, 4);

        p.setARGB(255, 255, 255, 255);

    }
   
    private void drawLandscape1(Canvas c, Rect bounds) {
        Paint p = new Paint();
        p.setARGB(255, 255, 255, 255);
       
        double conv = 4 * Math.PI / bounds.width();
        for (int i=0; i<=bounds.right; i++) {
            int height = (int) (30*Math.sin(i*conv) + 100);
            c.drawLine(i, bounds.bottom, i, bounds.bottom - height, p);
        }
       
        p.setARGB(255, 200, 200, 200);
        c.drawCircle(bounds.centerX(), bounds.bottom-100, 80, p);
        c.drawCircle(bounds.centerX(), bounds.bottom-200, 50, p);
        c.drawCircle(bounds.centerX(), bounds.bottom-265, 30, p);
       
        p.setColor(0xff000000);
        int x1 = bounds.centerX();
        int y1 = bounds.bottom-270;
       
        c.drawCircle(x1-12, y1, 5, p);
        c.drawCircle(x1+12, y1, 5, p);
       
        p.setColor(0xffcc9933);
        Path nose = new Path();
        nose.moveTo(x1, y1+4);
        nose.lineTo(x1+4, y1+18);
        nose.lineTo(x1-4, y1+18);
        nose.close();
       
        c.drawPath(nose, p);
    }
   
    private void drawLandscape2(Canvas c, Rect bounds) {
        Paint p = new Paint();
        p.setARGB(255, 255, 255, 255);
       
        c.drawRect(30, bounds.bottom-30, bounds.right-30, bounds.bottom, p);
        c.drawCircle(30, bounds.bottom, 30, p);
        c.drawCircle(bounds.right-30, bounds.bottom, 30, p);
       
        drawTree(c, p, bounds.centerX(), bounds.bottom - 90, 200, 90, 4);
        p.setARGB(255, 255, 255, 255);

    }
   
    private void drawLandscape(int n, Canvas c, Rect bounds) {
        switch (n) {
            case 0: drawLandscape0(c, bounds);
                    break;
            case 1: drawLandscape1(c, bounds);
                    break;
            case 2: drawLandscape2(c, bounds);
                    break;
        }
    }
   
    int getRandomX() {
        return (int) (currentSize.width()*Math.random());
    } 
   
    class Particle {
        public int x;
        public int y;
        public int size;
    }
   
    class SnowThread extends Thread {
       
        private boolean genLandscape;
        private boolean exit;
       
        public SnowThread(boolean gen) {
            particles = new ArrayList<Particle>();
            snowHitGround = false;
            genLandscape = gen;
            exit = false;
        }
       
        public void quit() {
            exit = true;
        }
       
        @Override
        public void run() {
            inited = false;
           
            if (genLandscape) {
                Canvas canvas = new Canvas(buffer);
                int landscape = (int) (MAX_LANDSCAPES*Math.random());
                drawLandscape(landscape, canvas, currentSize);
            }
           
            // create height field.
            for (int x=0; x<buffer.getWidth(); x++) {
                int y = 0;
                while (y<buffer.getHeight() && (buffer.getPixel(x, y) & 0x00ffffff)==0)
                    y++;
                ctr[x] = y;
            }
           
            inited = true;
            int counter = 0;
            generateNew = 1;
           
            if (pd != null && pd.isShowing())
                pd.dismiss();
           
            while (!exit) {
                try {
                    Thread.sleep(10*(4-generateNew));
                    counter++;
                    if (counter < 1000) {
                        generateNew = 1;
                    } else if (counter < 3000) {
                        generateNew = 2;
                    } else  {
                        generateNew = 3;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               
                snowView.postInvalidate();
            }
        }
    }
   
    private void restart() {
        int w = buffer.getWidth();
        int h = buffer.getHeight();
       
        if (buffer != null)
            buffer.recycle();

        buffer = Bitmap.createBitmap(w, h, Config.RGB_565);
        ctr = new float[w];    
        restartSnowThread(true);
    }
   
    class SnowView extends View {
       
        public SnowView(Activity activity) {
            super(activity);
        }
       
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            if (snowThread != null)
                snowThread.quit();
           
            currentSize = new Rect(0,0,w-1,h-1);

            if (buffer != null)
                buffer.recycle();

            buffer = Bitmap.createBitmap(w, h, Config.RGB_565);
            ctr = new float[w];        
            restartSnowThread(true);
        }
       
        private void animate(Canvas canvas) {
            ArrayList<Particle> removals = new ArrayList<Particle>();
            int maxX = currentSize.width();
            for (Particle p: particles) {
                //canvas.drawRect(p.x, p.y, p.x+p.size, p.y+p.size, mPaint);
                canvas.drawCircle(p.x, p.y, p.size, mPaint);
                if (p.y >= ctr[p.x]) {
                    removals.add(p);
                    snowHitGround = true;
                } else {
                    double n = Math.random();
                    if (n < 0.25) {
                        p.x -= 2;
                        if (p.x < 0)
                            p.x = 0;
                    } else if (n < 0.5) {
                        p.x += 2;
                        if (p.x >= maxX)
                            p.x = 0;
                    }
                    p.y+= (3 + p.size);
                }
            }
           
            if (snowHitGround) {
                for (int i=0; i<8; i++) {
                    int x = getRandomX();
                   
                    if (x>0 & x<maxX) {
                        float prev = ctr[x-1];
                        float next = ctr[x+1];
                        float h = ctr[x];
                       
                        if (h>=prev && h>=next) {
                        } else if (h>=prev && h<=next) { // roll right
                            x=x+1;
                        } else { // roll left
                            x=x-1;
                        }
                       
                        ctr[x]--;
                        buffer.setPixel(x, (int) ctr[x], 0xffffffff);
                    }
                }
            }
           
            particles.removeAll(removals);
           
            for (int i=0; i<generateNew; i++) {
                Particle p = new Particle();
                p.x = getRandomX();
                p.y = 0;
                p.size = (int) (3*Math.random());
                particles.add(p);
            }       
           
            //mPaint.setARGB(255, 0,0,0);
            //canvas.drawText(imageFilePath, 10, buffer.getHeight()-30, mPaint);
           
            mPaint.setARGB(255, 255,255,255);
        }
       
        @Override
        protected void onDraw(Canvas canvas) {

            if (!inited) {
                mPaint.setARGB(255, 255, 255, 255);
                mPaint.setTextSize(24);
                canvas.drawText("Initializing...", 100, currentSize.height()/2-20, mPaint);
                canvas.drawText("Please wait!", 100, currentSize.height()/2+10, mPaint);
               
            } else {
                canvas.drawBitmap(buffer, 0, 0, mPaint);
                animate(canvas);
            }
        }       
       
    }
   
    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_NEW, 0, "New");
        menu.add(0, MENU_PIC, 0, "Picture");
        menu.add(0, MENU_ABOUT, 0, "About");
        menu.add(0, MENU_QUIT, 0, "Quit");
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            restart();
            return true;
        case MENU_PIC:
            startActivityForResult(new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI),
                    REQUEST_FILEPICK);
            return true;

        case MENU_ABOUT:
            showDialog(DIALOG_ABOUT);
            return true;
        case MENU_QUIT:
            System.exit(0);
            return true;
        }
        return false;
    }
   
    private Bitmap threshold(Bitmap src) {
        Bitmap target = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Config.ARGB_8888);
       
        int prevLum = 0;
        int scan[] = new int[src.getWidth()];
       
        for (int x=0; x<src.getWidth(); x++) {
            scan[x] = src.getHeight();
            for (int y=0; y<src.getHeight(); y++) {
                int p = src.getPixel(x,y);
                int r = (p & 0xff0000) >> 16;
                int g = (p & 0xff00) >> 8;
                int b = p & 0xff;
                int lum = Math.round(0.299f * r + 0.587f * g + 0.114f * b);
                if (y>0) {
                    int diff = Math.abs(lum - prevLum);
                    if (diff > 10) {
                        target.setPixel(x, y, 0xffffffff);
                        for (int y1=y; y1<src.getHeight(); y1++) {
                            target.setPixel(x, y1, src.getPixel(x,y1));
                        }
                        scan[x] = y;
                        break;
                    } else {
                        target.setPixel(x, y, 0xff000000);
                    }
                }
                prevLum = lum;

            }
        }
       
        // remove outliers
        int scan1[] = new int[src.getWidth()];
        for (int i=0; i<src.getWidth()-1; i++)
            scan1[i] = scan[i]-scan[i+1];
   
        for (int i=0; i<src.getWidth()-2; i++) {
            int diff2 = scan1[i]-scan1[i+1];
            if (diff2 < 0) {
                for (int y1=0; y1<scan[i]; y1++) {
                    target.setPixel(i+2, y1, 0xff000000);
                }
            }
        }
       
        return target;
    }
   
    private void restartSnowThread(boolean genLandscape) {
        if (snowThread != null) {
            snowThread.quit();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           
        }
        snowThread = new SnowThread(genLandscape);
        snowThread.start();
    }
   
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILEPICK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                Cursor cursor = getContentResolver().query(uri, new String[]{
                    android.provider.MediaStore.Images.ImageColumns.DATA },null, null, null);

                cursor.moveToFirst();
                imageFilePath = cursor.getString(0);
                cursor.close();
               
                Bitmap bmp = BitmapFactory.decodeFile(imageFilePath);
               
                int maxh = (int) (currentSize.height() * 0.5);
                int maxw = (int) (currentSize.width() * 0.7);
               
                int hi = bmp.getHeight();
                int wi = bmp.getWidth();
               
                boolean c1 = (wi > hi);
                boolean c2 = (maxw > maxh);
               
                int h,w;
                if (c1) {
                    w = maxw;
                    h = (int) ((hi * maxw)/wi);
                } else {
                    h = maxh;
                    w = (int) ((wi * maxh)/hi);
                }
               
                final Bitmap bmp1 = Bitmap.createScaledBitmap(bmp, w, h, false);
               
                pd = ProgressDialog.show(this, "Working", "Analyzing picture...",
                        true, false);
               
                new Thread() {
                    public void run() {
                       
                        if (snowThread != null)
                            snowThread.quit();
                       
                        int w = currentSize.width();
                        int h = currentSize.height();

                        Bitmap edges = threshold(bmp1);

                        buffer = Bitmap.createBitmap(w, h, Config.ARGB_8888);
                        ctr = new float[1000];        
                        Canvas c = new Canvas(buffer);
                        c.drawRect(0, h-20, w-1, h-1, mPaint);
                        c.drawBitmap(edges, (int) (w-bmp1.getWidth())/2,
                                (int) (h-20-bmp1.getHeight()), mPaint);
                       
                        restartSnowThread(false);

                    };
                }.start();

            }
        }
    }
   
   
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Let it Snow v1.0\n(c) Raj Madhuram, 2009\n\nThis is a socialware. Please visit developer page for more info.\n\nhttp://geekraj.com")
               .setCancelable(false)
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        AlertDialog alert = builder.create();
        return alert;
    }
  
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);      
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN );
        snowView = new SnowView(this);
        setContentView(snowView);
       
    }
   
}
