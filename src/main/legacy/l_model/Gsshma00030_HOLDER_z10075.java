
@DzModel(name = "Gsshma00030_HOLDER_z10075", desc = "주주팝업조회", tableName = "")
public class Gsshma00030_HOLDER_z10075 extends DzAbstractModel {

	@SerializedName("EMS_STOCK_HOLDER_ID")
	@DzModelField(name="ems_stock_holder_id", desc="주주등록ID", colName="EMS_STOCK_HOLDER_ID", colSize="22", isKey=false)
	private BigDecimal ems_stock_holder_id;
	  
	@SerializedName("HOLDER_NO")
	@DzModelField(name="holder_no", desc="주민(사업자)번호", colName="HOLDER_NO", colSize="14", isKey=false)
	private String holder_no;
	  
	@SerializedName("HOLDER_NM")
	@DzModelField(name="holder_nm", desc="주주명", colName="HOLDER_NM", colSize="100", isKey=false)
	private String holder_nm;
	  
	@SerializedName("STA_YMD")
	@DzModelField(name="sta_ymd", desc="시작일", colName="STA_YMD", colSize="0", isKey=false)
	private String sta_ymd;
	  
	@SerializedName("END_YMD")
	@DzModelField(name="end_ymd", desc="종료일", colName="END_YMD", colSize="0", isKey=false)
	private String end_ymd;
	  
	@SerializedName("HOL_PHOTO")
	@DzModelField(name="hol_photo", desc="사진파일경로", colName="HOL_PHOTO", colSize="22", isKey=false)
	private String hol_photo;
	  
	@SerializedName("HOL_STAMP")
	@DzModelField(name="hol_stamp", desc="인감파일경로", colName="HOL_STAMP", colSize="22", isKey=false)
	private String hol_stamp;
	  
	@SerializedName("NOTE")
	@DzModelField(name="note", desc="비고", colName="NOTE", colSize="100", isKey=false)
	private String note;
	  
	@SerializedName("MOD_USER_ID")
	@DzModelField(name="mod_user_id", desc="수정자", colName="MOD_USER_ID", colSize="22", isKey=false)
	private BigDecimal mod_user_id;
	  
	@SerializedName("MOD_DATE")
	@DzModelField(name="mod_date", desc="수정일시", colName="MOD_DATE", colSize="0", isKey=false)
	private String mod_date;

	public BigDecimal getEms_stock_holder_id() {
		return ems_stock_holder_id;
	}

	public void setEms_stock_holder_id(BigDecimal ems_stock_holder_id) {
		this.ems_stock_holder_id = ems_stock_holder_id;
	}
 
	public String getHolder_no() {
		return holder_no;
	}

	public void setHolder_no(String holder_no) {
		this.holder_no = holder_no;
	}
 
	public String getHolder_nm() {
		return holder_nm;
	}

	public void setHolder_nm(String holder_nm) {
		this.holder_nm = holder_nm;
	}
 
	public String getSta_ymd() {
		return sta_ymd;
	}

	public void setSta_ymd(String sta_ymd) {
		this.sta_ymd = sta_ymd;
	}
 
	public String getEnd_ymd() {
		return end_ymd;
	}

	public void setEnd_ymd(String end_ymd) {
		this.end_ymd = end_ymd;
	}
 
	public String getHol_photo() {
		return hol_photo;
	}

	public void setHol_photo(String hol_photo) {
		this.hol_photo = hol_photo;
	}
 
	public String getHol_stamp() {
		return hol_stamp;
	}

	public void setHol_stamp(String hol_stamp) {
		this.hol_stamp = hol_stamp;
	}
 
	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
 
	public BigDecimal getMod_user_id() {
		return mod_user_id;
	}

	public void setMod_user_id(BigDecimal mod_user_id) {
		this.mod_user_id = mod_user_id;
	}
 
	public String getMod_date() {
		return mod_date;
	}

	public void setMod_date(String mod_date) {
		this.mod_date = mod_date;
	}
}