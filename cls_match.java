/*
   cls_match.java
   copyright(c) web_international
   1996.05.28 kyuhyun park
*/

import java.util.Date;
import java.net.*;
import java.awt.*;
import java.awt.image.*;
import java.applet.*;

class cls_image_observer implements ImageObserver
{
   cls_match match;

   public cls_image_observer(cls_match arg_match)
   {
      match = arg_match;
   }

   public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h)
   {
      return match.imageUpdate(img, infoflags, x, y, w, h);
   }
}

public class cls_match extends Applet implements Runnable
{
   final static int cols = 6;
   final static int cell_size = 60;
   final static int cell_cnt = cols * cols;
   final static int word_image_cnt = cell_cnt / 2;
   final static int cmd_line_height = 30;
   final static int reset_inx = 1024;

   Thread bg_thread;
   cls_image_observer observer = new cls_image_observer(this);
   boolean paintable_flg = false;

   Image push_image;
   Image pull_image;
   Image reset_pull_image;
   Image reset_push_image;
   Image word_image_ary[] = new Image[word_image_cnt];

   int image_inx_ary[] = new int[cell_cnt];
   boolean match_flg_ary[] = new boolean[cell_cnt];
   int open_cell_cnt;
   int hit_pair_cnt;
   int cell1;
   int cell2;
   int mouse_down_inx;
   int pushed_cell_inx;
   boolean reset_pushed_flg;
   Date open_time = new Date();

   public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
   {
      boolean rc = false;

      if ((infoflags & ImageObserver.ALLBITS) != 0)
      {
         if (img == pull_image || img == reset_pull_image)
            repaint();
      }
      else
      {
         rc = true;
      }

      return rc;
   }

   public void init()
   {
      int i,cnt;
      Graphics g = getGraphics();

      resize(cell_size * cols, cell_size * cols + cmd_line_height);

      pull_image = getImage(getCodeBase(), "image/up.gif");
      push_image = getImage(getCodeBase(), "image/down.gif");
      reset_pull_image = getImage(getCodeBase(), "image/reset_up.gif");
      reset_push_image = getImage(getCodeBase(), "image/reset_down.gif");

      g.drawImage(push_image, 0, 1024, observer);
      g.drawImage(pull_image, 0, 1024, observer);
      g.drawImage(reset_pull_image, 0, 1024, observer);
      g.drawImage(reset_push_image, 0, 1024, observer);

      for (i = 0; i < word_image_cnt; i++)
      {
         String name = "image/word" + (i < 10 ? "0" : "") + i + ".gif";
         word_image_ary[i] = getImage(getCodeBase(), name);
         g.drawImage(word_image_ary[i], 0, 1024, observer);
      }

      bg_thread = new Thread(this);
      bg_thread.start();

      reset();
   }

   public void start()
   {
   }

   public void stop()
   {
   }

   public void destroy()
   {
   }

   public void update(Graphics g)
   {
      paint(g);
   }

   synchronized public void paint(Graphics g)
   {
      if (paintable_flg)
      {
         int x;
         int y;
         int inx;
         Image image;

         for (y = 0; y < cols ; y++)
         {
            for (x = 0; x < cols ; x++)
            {
               inx = y * cols + x;
               image = null;

               if (inx == cell1 || inx == cell2 || match_flg_ary[inx])
               {
                  image = word_image_ary[image_inx_ary[inx]];
               }
               else if (inx == pushed_cell_inx)
               {
                  image = push_image;
               }
               else
               {
                  image = pull_image;
               }

               draw_cell(g, inx, image);
            }
         }
         g.drawImage(reset_pull_image, 0, cols * cell_size, null);
      }
   }

   public synchronized boolean mouseDown(Event event, int x, int y)
   {
      mouse_down_inx = get_button_inx(x, y);

      if (mouse_down_inx == reset_inx)
      {
         push_reset();
      }
      else if (is_closed_cell(mouse_down_inx))
      {
         push_cell(mouse_down_inx);
      }
      else
      {
         reset_pushed_flg = false;
         pushed_cell_inx = -1;
      }

      return true;
   }

   public synchronized boolean mouseUp(Event event, int x, int y)
   {
      int inx = get_button_inx(x, y);

      if (inx == mouse_down_inx)
      {
         if (inx == reset_inx)
         {
            pull_reset();
            reset();
         }
         else
         {
            if (pushed_cell_inx != -1)
            {
               pull_cell();
            }

            process_cell(inx);
         }
      }
      else
      {
         pull_reset();
         pull_cell();
      }

      return true;
   }

   public boolean mouseExit(Event event, int x, int y)
   {
      pull_reset();
      pull_cell();
      return true;
   }

   boolean is_closed_cell(int inx)
   {
      return inx >= 0 && inx < cell_cnt && !match_flg_ary[inx] && inx != cell1 && inx != cell2;
   }

   int get_button_inx(int x, int y)
   {
      return y < cols * cell_size ? (y / cell_size) * cols + (x / cell_size) : reset_inx;
   }

   void process_cell(int inx)
   {
      if (!match_flg_ary[inx])
      {
         if (open_cell_cnt == 2)
         {
            pull_cells();
         }

         if (open_cell_cnt == 0)
         {
            open_cell1(inx);
         }
         else
         {
            if (inx == cell1)
            {
               pull_cells();
            }
            else
            {
               open_cell2(inx);
            }
         }
      }
   }

   void open_cell1(int inx)
   {
      cell1 = inx;
      open_cell(cell1);
      open_cell_cnt++;
      open_time = new Date();
   }

   void open_cell2(int inx)
   {
      cell2 = inx;
      open_cell(cell2);
      open_cell_cnt++;
      open_time = new Date();

      if (image_inx_ary[cell1] == image_inx_ary[cell2])
      {
         match_flg_ary[cell1] = true;
         match_flg_ary[cell2] = true;
         cell1 = -1;
         cell2 = -1;
         open_cell_cnt = 0;

         hit_pair_cnt++;

         if (hit_pair_cnt == word_image_cnt)
         {
            cong();
         }
      }
   }

   void pull_cells()
   {
      if (cell1 != -1)
      {
         draw_cell(cell1, pull_image);
         cell1 = -1;
      }
      if (cell2 != -1)
      {
         draw_cell(cell2, pull_image);
         cell2 = -1;
      }
      open_cell_cnt = 0;
   }

   void open_cell(int inx)
   {
      draw_cell(inx, word_image_ary[image_inx_ary[inx]]);
   }

   void pull_cell()
   {
      if (pushed_cell_inx != -1)
      {
         draw_cell(pushed_cell_inx, pull_image);
         pushed_cell_inx = -1;
         sleep(100);
      }
   }

   void push_cell(int inx)
   {
      draw_cell(inx, push_image);
      pushed_cell_inx = inx;
   }

   void draw_cell(int inx, Image image)
   {
      draw_cell(getGraphics(), inx, image);
   }

   void draw_cell(Graphics g, int inx, Image image)
   {
      int x = inx % cols;
      int y = inx / cols;

      g.drawImage(image, x * cell_size, y * cell_size, null);
   }

   void erase_cell(int inx)
   {
      Graphics g = getGraphics();

      int x = inx % cols;
      int y = inx / cols;

      g.setColor(Color.white);
      g.fillRect(x * cell_size, y * cell_size, cell_size, cell_size);
   }

   void push_reset()
   {
      draw_reset(reset_push_image);
      reset_pushed_flg = true;
   }

   void pull_reset()
   {
      if (reset_pushed_flg)
      {
         draw_reset(reset_pull_image);
         reset_pushed_flg = false;
         sleep(100);
      }
   }

   void draw_reset(Image image)
   {
      getGraphics().drawImage(image, 0, cols * cell_size, null);
   }

   void cong()
   {
      reset();
   }

   synchronized void reset()
   {
      int i;

      for (i = 0; i < cell_cnt; i++)
      {
         image_inx_ary[i] = -1;
         match_flg_ary[i] = false;
      }

      for (i = 0; i < cell_cnt; i++)
      {
         int image_inx = i / 2;
         int cell_inx = (int)(Math.random() * (cell_cnt - 0.01));
         while (image_inx_ary[cell_inx] != -1)
         {
            cell_inx = (cell_inx + 1) % cell_cnt;
         }
         image_inx_ary[cell_inx] = image_inx;
      }

      open_cell_cnt = 0;
      hit_pair_cnt = 0;
      cell1 = -1;
      cell2 = -1;
      mouse_down_inx = -1;
      pushed_cell_inx = -1;
      reset_pushed_flg = false;

      paintable_flg = true;

      repaint();
   }

   public void run()
   {
      while (true)
      {
         sleep(1024);
         check_time();
      }
   }

   synchronized void check_time()
   {
      int s1 = open_time.getSeconds();
      int s2 = new Date().getSeconds();
      if (s1 > s2) s2 += 60;

      if (open_cell_cnt > 0 && s2 - s1 > 2)
      {
         pull_cells();
      }
   }

   void sleep(int ms)
   {
      try
      {
         Thread.sleep(ms);
      }
      catch (InterruptedException e)
      {
      }
   }
}

