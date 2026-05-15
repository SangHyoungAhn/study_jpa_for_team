
public class DzAbstractModel {
	@JsonProperty(access = Access.READ_ONLY)
	private transient String dbname;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String DbProviderGroup;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String DbProvider;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_INSERT_ID;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_INSERT_IP;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_INSERT_MCADDR_NM;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_UPDATE_ID;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_UPDATE_IP;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_UPDATE_MCADDR_NM;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_INSERT_DTS;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_UPDATE_DTS;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_CM_SYSDATE;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_CM_YMD;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_CM_END_YMD;
	@JsonProperty(access = Access.READ_ONLY)
	private transient String P_CM_LANG_CD;

	public String getDbname() {
		return this.dbname;
	}

	public void setDbname(String dbname) {
		this.dbname = dbname;
	}

	public String getDbProviderGroup() {
		return this.DbProviderGroup;
	}

	public void setDbProviderGroup(String dbProviderGroup) {
		this.DbProviderGroup = dbProviderGroup;
	}

	public String getDbProvider() {
		return this.DbProvider;
	}

	public void setDbProvider(String dbProvider) {
		this.DbProvider = dbProvider;
	}

	public String getP_INSERT_ID() {
		return this.P_INSERT_ID;
	}

	public void setP_INSERT_ID(String p_INSERT_ID) {
		this.P_INSERT_ID = p_INSERT_ID;
	}

	public String getP_INSERT_IP() {
		return this.P_INSERT_IP;
	}

	public void setP_INSERT_IP(String p_INSERT_IP) {
		this.P_INSERT_IP = p_INSERT_IP;
	}

	public String getP_INSERT_MCADDR_NM() {
		return this.P_INSERT_MCADDR_NM;
	}

	public void setP_INSERT_MCADDR_NM(String p_INSERT_MCADDR_NM) {
		this.P_INSERT_MCADDR_NM = p_INSERT_MCADDR_NM;
	}

	public String getP_UPDATE_ID() {
		return this.P_UPDATE_ID;
	}

	public void setP_UPDATE_ID(String p_UPDATE_ID) {
		this.P_UPDATE_ID = p_UPDATE_ID;
	}

	public String getP_UPDATE_IP() {
		return this.P_UPDATE_IP;
	}

	public void setP_UPDATE_IP(String p_UPDATE_IP) {
		this.P_UPDATE_IP = p_UPDATE_IP;
	}

	public String getP_UPDATE_MCADDR_NM() {
		return this.P_UPDATE_MCADDR_NM;
	}

	public void setP_UPDATE_MCADDR_NM(String p_UPDATE_MCADDR_NM) {
		this.P_UPDATE_MCADDR_NM = p_UPDATE_MCADDR_NM;
	}

	public String getP_INSERT_DTS() {
		return this.P_INSERT_DTS;
	}

	public void setP_INSERT_DTS(String P_INSERT_DTS) {
		this.P_INSERT_DTS = P_INSERT_DTS;
	}

	public String getP_UPDATE_DTS() {
		return this.P_UPDATE_DTS;
	}

	public void setP_UPDATE_DTS(String p_UPDATE_DTS) {
		this.P_UPDATE_DTS = p_UPDATE_DTS;
	}

	public String getP_CM_SYSDATE() {
		return this.P_CM_SYSDATE;
	}

	public void setP_CM_SYSDATE(String p_CM_SYSDATE) {
		this.P_CM_SYSDATE = p_CM_SYSDATE;
	}

	public String getP_CM_YMD() {
		return this.P_CM_YMD;
	}

	public void setP_CM_YMD(String p_CM_YMD) {
		this.P_CM_YMD = p_CM_YMD;
	}

	public String getP_CM_END_YMD() {
		return this.P_CM_END_YMD;
	}

	public void setP_CM_END_YMD(String p_CM_END_YMD) {
		this.P_CM_END_YMD = p_CM_END_YMD;
	}

	public String getP_CM_LANG_CD() {
		return this.P_CM_LANG_CD;
	}

	public void setP_CM_LANG_CD(String p_CM_LANG_CD) {
		this.P_CM_LANG_CD = p_CM_LANG_CD;
	}
}