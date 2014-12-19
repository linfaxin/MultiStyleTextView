package com.linfaxin.multistyletextview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by linfaxin on 2014/8/13 013.
 * Email: linlinfaxin@163.com
 *
 * 颜色：//#ffffff
 * 类似这样规定颜色样式的开始
 * android:text="//#ff5ec353好评//：%d条   //#f99c2e中评//：%d条   //#e75d5d差评//：%d条"
 *
 * 字体大小：//S16  //%80
 * 类似这样规定额外的字体大小样式的开始，单位DP/百分比
 * android:text="//s12好评//：1条   //s13中评//：1条   //s14差评//：1条"
 *
 * 下划线： //u
 * 删除线： //l
 * 加粗： //b
 *
 * 可以用//#!/，//S!/，//U!/...来标识对应样式的结束，用//标志所有样式的结束
 *
 * 混合以上的使用
 */
public class MultiStyleTextView extends TextView {
    private static HashSet<Class<? extends StyleText>> stylesClasses;
    private static final String styleSeparator = "//";
    private static final String styleEndSeparator = "!/";
    private static final String refStart = "@";

    private ColorStateList[] colors = new ColorStateList[6];
    private int[] sizes = new int[3];
    String format;

    public MultiStyleTextView(Context context) {
        super(context);
        init(null);
    }

    public MultiStyleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MultiStyleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs){
        if(attrs!=null){
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MultiStyleTextView);
            colors[0]= a.getColorStateList(R.styleable.MultiStyleTextView_color1);
            colors[1]= a.getColorStateList(R.styleable.MultiStyleTextView_color2);
            colors[2]= a.getColorStateList(R.styleable.MultiStyleTextView_color3);
            colors[3]= a.getColorStateList(R.styleable.MultiStyleTextView_color4);
            colors[4]= a.getColorStateList(R.styleable.MultiStyleTextView_color5);
            colors[5]= a.getColorStateList(R.styleable.MultiStyleTextView_color6);
            for(int i=0,length=colors.length; i<length; i++){
                if(colors[i]==null) colors[i] = getTextColors();
            }

            sizes[0]= a.getDimensionPixelSize(R.styleable.MultiStyleTextView_size1, 14);
            sizes[1]= a.getDimensionPixelSize(R.styleable.MultiStyleTextView_size2, 14);
            sizes[2]= a.getDimensionPixelSize(R.styleable.MultiStyleTextView_size3, 14);

            format = a.getString(R.styleable.MultiStyleTextView_format);
        }
        CharSequence text = getText();
        if(text!=null && text.length() > 0){
            setTextMulti(text.toString());
        }else if(format != null ) setTextMulti(format);

        if(getHint()!=null && getHint().length()>0 ) setHint(convertToMulti(getHint().toString()));
        requestLayout();
    }
    public void setColors(int... colors) {
        if(colors==null || colors.length==0) return;
        for(int i=0,length=colors.length;i<length;i++){
            this.colors[i] = ColorStateList.valueOf(colors[i]);
        }
    }
    public void setColors(ColorStateList... colors) {
        if(colors==null || colors.length==0) return;
        this.colors = colors;
    }

    private String getFormattedText(Object... args){
        try {
            return String.format(format, args);
        } catch (Exception e) {
            //更好的兼容性的format
            try {
                if(format!=null){
                    int wantArgsLength = format.split("%").length -1;
                    Object[] fixArgs = Arrays.copyOf(args, wantArgsLength);

                    for(int i=0; i<wantArgsLength; i++){
                        Object arg = fixArgs[i];
                        if(arg instanceof Number){
                            fixArgs[i] = arg+"";
                        }
                    }
                    return String.format(format.replace("%d", "%s").replace("%f", "%s"), fixArgs);
                }
            } catch (Exception ignore) {
            }
            e.printStackTrace();
        }
        return format;
    }
    public void formatText(Object... args){
        if(format==null) format = getText().toString();
        setTextMulti(getFormattedText(args));
    }
    public void setTextFormat(String format, Object... args){
        this.format = format;
        if(args.length>0) setTextMulti(getFormattedText(args));
        else setTextMulti(format);
    }
    public void setTextMulti(String text){
        setText(convertToMulti(text));
    }

    public Spannable convertToMulti(String text){
        //解析出多个部分
        String[] parts = text.split(styleSeparator);//用//来规定后面的字体的格式
        ArrayList<StyleText> styleTexts = new ArrayList<StyleText>();
        for(String part : parts){
            if(TextUtils.isEmpty(part)){
                continue;
            }

            styleTexts.add(parseStyleText(part));
        }

        //获得所有过滤掉符号的内容
        StringBuilder sb = new StringBuilder();
        for(StyleText colorText : styleTexts){
            colorText.start = sb.length();
            sb.append(colorText.text);
        }
        Spannable spannable = new SpannableString(sb);

        //开始设置效果
        HashMap<Class, StyleText> lastSetStyleMap = new HashMap<Class, StyleText>();
        for(StyleText styleText : styleTexts){
            int nowIndex = styleText.start;
            if(styleText instanceof NoStyleText){//之前设置的所有style截止到这里
                for(StyleText lastStyle : lastSetStyleMap.values()){
                    lastStyle.setSpan(spannable, nowIndex);
                }
                lastSetStyleMap.clear();

            }else{
                StyleText lastStyle = lastSetStyleMap.get(styleText.getClass());
                if(lastStyle!=null){
                    lastStyle.setSpan(spannable, nowIndex);
                }
                lastSetStyleMap.put(styleText.getClass(), styleText);
            }
        }
        //效果设置到最后
        int endIndex = spannable.length();
        for(StyleText lastStyle : lastSetStyleMap.values()){
            lastStyle.setSpan(spannable, endIndex);
        }

        return spannable;
    }

    /**解析出一个style的部分 */
    private StyleText parseStyleText(String part){

        String styleFlag = part.substring(0,1);//取得首个styleFlag：#号或者S...
        part = part.substring(1);//先去掉首个可能的styleFlag

        Integer refIndex = null;
        if(part.startsWith(refStart)){//引用，解析@1,@3这样的形式。base-1
            try {
                refIndex = Integer.valueOf(part.substring(1, 2))-1;
                part = part.substring(2);
            } catch (Exception ignore) {
            }
        }
        if(stylesClasses == null){
            initStyleClasses(getContext());
        }
        for(Class<? extends StyleText> c : stylesClasses){
            Style style = c.getAnnotation(Style.class);
            if(style==null){
                Log.e(MultiStyleTextView.class.getSimpleName(), "class:" + c + " miss annotation:" + Style.class);
            }else{
                if(styleFlag.equalsIgnoreCase(style.flag())){
                    try {
                        StyleText styleText = c.newInstance();
                        styleText.doParse(MultiStyleTextView.this, part, styleFlag, refIndex);
                        return styleText;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new NoStyleText(part, styleFlag, refIndex);
                    }
                }
            }
        }
        return new NoStyleText(part, styleFlag, refIndex);
    }
    public void removeAllStyle(){
        if(getText()!=null) setText(getText().toString());
        if(getHint()!=null) setHint(getHint().toString());
    }

    public void removeAllStyle(Class<? extends StyleText> c){
        try {
            SpannableString spannable = new SpannableString(getText());
            for(Object whatSpan : getAllSpans(spannable, c)){
                if(whatSpan!=null) spannable.removeSpan(whatSpan);
            }
            setText(spannable);

            if(getHint()!=null){
                spannable = new SpannableString(getHint());
                for(Object whatSpan : getAllSpans(spannable, c)){
                    if(whatSpan!=null) spannable.removeSpan(whatSpan);
                }
                setHint(spannable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object[] getAllSpans(Spannable spannable, Class<? extends StyleText> c){
        return spannable.getSpans(0, spannable.length(), getSpanClass(c));
    }

    private static Class getSpanClass(Class<? extends StyleText> c){
        Style style = c.getAnnotation(Style.class);
        if(style==null) return null;
        return style.spanClass();
    }
    private static synchronized void initStyleClasses(Context context){
        if(stylesClasses==null){
            stylesClasses = new HashSet<Class<? extends StyleText>>();
            stylesClasses.add(ColorText.class);
            stylesClasses.add(SizeText.class);
            stylesClasses.add(UnderlineStyle.class);
            stylesClasses.add(LineThroughStyle.class);
            stylesClasses.add(BoldStyle.class);
        }
    }

    private static abstract class StyleText{
        String text;
        int start;
        boolean isIgnoreStyle;
        public StyleText(){
        }

        public final void setSpan(Spannable span, int end){
            if(isIgnoreStyle) return;
            Object spanObject = getSpanObject();
            if(spanObject==null) return;
            span.setSpan(spanObject, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        public void doParse(MultiStyleTextView tv, String part, String styleFlag, Integer refIndex) throws Exception{
            if(refIndex==null && part!=null && part.startsWith(styleEndSeparator)){
                isIgnoreStyle = true;
                text = part.substring(styleEndSeparator.length());
            }else{
                parse(tv, part, styleFlag, refIndex);
            }
        }
        /**
         * @param part 未解析的文字,已经去除开头的styleFlag和可能的refIndex
         * @param styleFlag 样式的缩略标识,注解在类的开头{@link Style}.
         * @param refIndex 可能的资源引用,指定当前样式的序号
         * @throws Exception
         */
        public abstract void parse(MultiStyleTextView tv, String part, String styleFlag,@Nullable Integer refIndex) throws Exception;
        /**获得可以被setSpan的对象 */
        public abstract Object getSpanObject();
    }
    //无需参数的样式
    private static abstract class NoParamStyleText extends StyleText{
        @Override
        public void parse(MultiStyleTextView tv, String part, String styleFlag,@Nullable Integer refIndex){
            if(refIndex!=null){
                this.text = refStart + refIndex + part;
            }else{
                this.text = part;
            }
        }
    }
    //无样式
    private class NoStyleText extends StyleText{
        public NoStyleText(String part) {
            parse(MultiStyleTextView.this, part, null, null);
        }
        public NoStyleText(String part, String styleFlag, @Nullable Integer refIndex) {
            parse(MultiStyleTextView.this, part, styleFlag, refIndex);
        }

        @Override
        public void parse(MultiStyleTextView tv, String part, String styleFlag,@Nullable Integer refIndex){
            if(styleFlag==null) styleFlag="";
            if(refIndex!=null){
                this.text = styleFlag + refStart + refIndex + part;
            }else{
                this.text = styleFlag + part;
            }
        }

        @Override
        public Object getSpanObject() {
            return null;
        }
    }

    //字体颜色
    @Style(flag = "#", spanClass = ColorText.ForegroundColorListSpan.class)
    public static class ColorText extends StyleText{
        ColorStateList colorStateList;
        @Override
        public void parse(MultiStyleTextView tv, String part, String styleFlag, @Nullable Integer refIndex) throws Exception {
            ColorStateList textColor = null;
            ColorStateList[] colors = tv.colors;
            if(refIndex !=null && colors!=null && colors.length>0){//引用
                textColor = colors[refIndex % colors.length];
                if(textColor==null) textColor = tv.getTextColors();

            }else{
                try {
                    textColor = ColorStateList.valueOf(Color.parseColor("#" + part.substring(0, 8).trim()));//前8个字符是颜色代码，代表字体颜色
                    part = part.substring(8);
                } catch (Exception ignore) {
                }
                if(textColor==null){
                    try {
                        textColor = ColorStateList.valueOf(Color.parseColor("#" + part.substring(0, 6).trim()));//前6个字符是颜色代码，代表字体颜色
                        part = part.substring(6);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            colorStateList = textColor;
            text = part;
        }

        @Override
        public Object getSpanObject() {
            if(colorStateList==null) return null;
            return new ForegroundColorListSpan(colorStateList);
        }

        class ForegroundColorListSpan extends ForegroundColorSpan {
            ColorStateList colorStateList;
            public ForegroundColorListSpan(ColorStateList colorStateList) {
                super(colorStateList.getDefaultColor());
                this.colorStateList = colorStateList;
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                if(colorStateList==null) ds.setColor(getForegroundColor());
                else ds.setColor(colorStateList.getColorForState(ds.drawableState, colorStateList.getDefaultColor()));
            }
        }
    }
    //字体大小
    @Style(flag = "s", spanClass = AbsoluteSizeSpan.class)
    public static class SizeText extends StyleText{
        Integer size;
        boolean isSizeInDp = true;
        @Override
        public void parse(MultiStyleTextView tv, String part, String styleFlag, @Nullable Integer refIndex) throws Exception {
            Integer size = null;
            int[] sizes = tv.sizes;
            isSizeInDp = true;

            if(refIndex !=null && sizes!=null && sizes.length>0){//引用
                size = sizes[refIndex % sizes.length];

            } else if(part.startsWith("%")){//支持百分比的设定://S%50
                try {
                    StringBuilder sb = new StringBuilder();
                    char c;
                    for(int i=1;i<=4;i++){
                        if(i>=part.length()) break;
                        c = part.charAt(i);
                        if( c >='0' && c <='9'){
                            sb.append(c);
                        }else break;
                    }
                    size = (int)(tv.getTextSize() * Double.parseDouble(sb.toString()) / 100);
                    part = part.substring(sb.length()+1);
                    isSizeInDp = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    size = Integer.valueOf(part.substring(0, 2).trim());//前两个字符是数字，代表字体大小
                    part = part.substring(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.size = size;
            text = part;
        }
        @Override
        public Object getSpanObject() {
            if(size ==null) return null;
            return new AbsoluteSizeSpan(size, isSizeInDp);
        }
    }
    //下划线
    @Style(flag = "u", spanClass = UnderlineSpan.class)
    public static class UnderlineStyle extends NoParamStyleText{
        @Override
        public Object getSpanObject() {
            return new UnderlineSpan();
        }

    }
    //删除线
    @Style(flag = "l", spanClass = StrikethroughSpan.class)
    public static class LineThroughStyle extends NoParamStyleText{
        @Override
        public Object getSpanObject() {
            return new StrikethroughSpan();
        }
    }
    //粗体
    @Style(flag = "b", spanClass = StyleSpan.class)
    public static class BoldStyle extends NoParamStyleText{
        @Override
        public Object getSpanObject() {
            return new StyleSpan(android.graphics.Typeface.BOLD);
        }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Style {
        public String flag();
        public Class spanClass();
    }
}
