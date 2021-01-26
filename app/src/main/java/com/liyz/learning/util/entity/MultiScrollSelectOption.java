package com.liyz.learning.util.entity;

import java.util.List;

public class MultiScrollSelectOption {
    public static final int STRING_LIST = 0;
    public static final int NUM_LIST = 1;
    private String append = "";

    private int contentType = 0;
    private int start = 0;
    private int end = 0;
    private List<String> stringList;
    private int size = 0;

    // 相对位置：选中的位置 从 0 开始
    private int selectPosition = -1;

    public MultiScrollSelectOption(){
    }
    public MultiScrollSelectOption(List<String> data, String append){
        this.contentType = STRING_LIST;
        this.stringList = data;
        if (append == null || append.length() == 0){
            this.append = "";
        }else{
            this.append = append;
        }
        if (data != null){
            this.size = data.size();
        }
    }
    public MultiScrollSelectOption(int start, int end, String append, int selectData){
        this(start,end,append);
        setSelectPosition(selectData);
    }
    public MultiScrollSelectOption(int start, int end, String append){
        this.contentType = NUM_LIST;
        this.start = start;
        this.end = end;
        this.size = end - start + 1;
        if (append == null || append.length() == 0){
            this.append = "";
        }else{
            this.append = append;
        }
    }

    public String getAppend() {
        return append;
    }

    public void setAppend(String append) {
        this.append = append;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.size = end - start + 1;
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.size = end - start + 1;
        this.end = end;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public int getSize() {
        return size;
    }

    /*public void setSize(int size) {
        this.size = size;
    }*/

    public int getSelectPosition() {
        //Logger.e("MultiScrollSelectOption","选中的位置为 = " + selectPosition);
        return selectPosition;
    }

    /**
     * 选中位置上显示的数字
     * @param selectData
     */
    public void setSelectPosition(int selectData) {
        if (selectData < start){
            this.selectPosition = 0;
        }else if(selectData > end){
            this.selectPosition = end - start;
        }else{
            this.selectPosition = selectData - start;
        }
    }

    /**
     * 相对start位置
     * @param position
     */
    public void setPosition(int position){
        if (position < 0){
            this.selectPosition = 0;
        }else if(position >= size){
            this.selectPosition = size -1;
        }else{
            this.selectPosition = position;
        }
    }

    public String getSelectResult(){
        return new StringBuilder().append(selectPosition + start).append(append).toString();
    }
    public int getPosition(int select){
        return select + start;
    }
    public String getSelectResult(int position){
        if (position < 0){
            position = 0;
        }else if(position > size - 1){
            position = size - 1;
        }
        return new StringBuilder().append(position + start).append(append).toString();
    }

    public String getShowText(int position){
        StringBuilder content = new StringBuilder();
        if (contentType == STRING_LIST){
            content.append(stringList.get(position)).append(append);
        }else{
            content.append(position + start).append(append);
        }
        return content.toString();
    }

    /**
     * 重置结束位置
     * @param end 结束位置
     * @param selectPosition 上一个选中位置（相对start，需要转换）
     */
    public void resetEnd(int end,int selectPosition) {
        this.end = end;
        this.size = end - start + 1;
        // 总数是发生变化的 这里需要重置下
        setPosition(selectPosition);
    }
    public void resetStart(int start,int selectPosition) {
        this.start = start;
        this.size = end - start + 1;
        // 总数是发生变化的 这里需要重置下
        setPosition(selectPosition);
    }
}
