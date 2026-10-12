package holylois.boombox;

import java.util.List;
public final class DefeatRecordTest {
    public static void main(String[] args){
        var first=record(20,40,false,100);var latest=record(10,80,false,200);var higher=record(50,30,false,300);
        check(!latest.strongerThan(first)&&higher.strongerThan(first),"level outranks health");
        check(record(20,50,false,200).strongerThan(first)&&record(20,40,true,200).strongerThan(first),"health then elite break level ties");
        check(!record(20,40,false,200).strongerThan(first),"exact ties retain first snapshot");
        var json=new com.google.gson.Gson();check(json.fromJson(json.toJson(first),DefeatRecord.class).equals(first),"snapshot persists without full NBT");
        try{record(20,Float.NaN,false,100);throw new AssertionError("invalid health accepted");}catch(IllegalArgumentException expected){}
        try{new DefeatRecord.Gear("body","minecraft:iron_helmet",-1,false,"","","","");throw new AssertionError("unsafe slot accepted");}catch(IllegalArgumentException expected){}
        try{new DefeatRecord.Gear("head","minecraft:iron_helmet",-1,false,"","","","x".repeat(1025));throw new AssertionError("oversized appearance accepted");}catch(IllegalArgumentException expected){}
        System.out.println("Strongest snapshot ranking, persistence and bounds: passed");
    }
    public static DefeatRecord record(int level,float health,boolean elite,long time){return new DefeatRecord("minecraft:zombie",level,health,elite,false,time,"minecraft:overworld",1,64,2,List.of(new DefeatRecord.Gear("head","minecraft:iron_helmet",-1,true,"","","","")),List.of(new DefeatRecord.Effect("minecraft:strength",1,200)),List.of("fiery"));}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
