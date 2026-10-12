package holylois.boombox;

/** One outer inset and one widget gap, with exact integer edges across every Journal column. */
public record JournalLayout(int width,int height,boolean wide,int content,int listWidth,int detailX,int detailWidth,
                            int bodyY,int bodyHeight,int actionsY,int footerY,int rows) {
    public static final int PAD=12,GAP=6,BUTTON=22;
    public static JournalLayout of(int width,int height){
        int content=width-2*PAD;boolean wide=width>=480;
        int list=wide?(content-GAP)*3/5:content,dx=wide?PAD+list+GAP:PAD,dw=wide?content-list-GAP:content;
        int footer=height-PAD-BUTTON,actions=footer-GAP-BUTTON,body=88,bh=Math.max(24,actions-GAP-body);
        return new JournalLayout(width,height,wide,content,list,dx,dw,body,bh,actions,footer,Math.clamp((bh+GAP)/30,1,8));
    }
    public int rowY(int index){return bodyY+(bodyHeight+GAP)*index/rows;}
    public int rowHeight(int index){return (bodyHeight+GAP)*(index+1)/rows-GAP-(bodyHeight+GAP)*index/rows;}
    public int filterX(int index,int count){int space=content-content/3-56;return PAD+content/3+56+(space+GAP)*index/count;}
    public int filterWidth(int index,int count){int space=content-content/3-56;return (space+GAP)*(index+1)/count-GAP-(space+GAP)*index/count;}
}
