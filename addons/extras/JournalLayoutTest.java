package holylois.boombox;

public final class JournalLayoutTest {
    public static void main(String[] args){
        for(int width:new int[]{379,403,639,640})for(int height:new int[]{216,310,400}){
            var l=JournalLayout.of(width,height);
            check(l.detailX()+l.detailWidth()==width-JournalLayout.PAD,"right column shares outer inset");
            check(l.footerY()+JournalLayout.BUTTON==height-JournalLayout.PAD,"footer shares bottom inset");
            int footerCount=l.wide()?3:4;
            check(l.detailFooterX(0,footerCount)==l.detailX(),"fish footer aligns with frame left");
            check(l.detailFooterX(footerCount-1,footerCount)+l.detailFooterWidth(footerCount-1,footerCount)==width-JournalLayout.PAD,"fish footer aligns with frame right");
            for(int i=1;i<footerCount;i++){
                check(l.detailFooterX(i,footerCount)-l.detailFooterX(i-1,footerCount)-l.detailFooterWidth(i-1,footerCount)==JournalLayout.GAP,"fish footer equal gaps");
                check(Math.abs(l.detailFooterWidth(i,footerCount)-l.detailFooterWidth(0,footerCount))<=1,"fish footer equal widths");
            }
            check(l.rowY(l.rows()-1)+l.rowHeight(l.rows()-1)==l.bodyY()+l.bodyHeight(),"last entry aligns with detail frame bottom");
            for(int i=1;i<l.rows();i++)check(l.rowY(i)-l.rowY(i-1)-l.rowHeight(i-1)==JournalLayout.GAP,"entry gap consistent");
            for(int count:new int[]{3,4}){
                check(l.filterX(count-1,count)+l.filterWidth(count-1,count)==width-JournalLayout.PAD,"last filter aligns with right inset");
                for(int i=1;i<count;i++)check(l.filterX(i,count)-l.filterX(i-1,count)-l.filterWidth(i-1,count)==JournalLayout.GAP,"filter gap consistent");
            }
        }
        System.out.println("Journal exact edges, responsive rows and consistent gaps: passed");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
