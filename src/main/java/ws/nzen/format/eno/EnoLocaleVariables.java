/** see ../../../../../LICENSE for release details */
package ws.nzen.format.eno;

/**  */
public enum EnoLocaleVariables
{
	LINE( "[LINE]", ",integer" ),
	NAME( "[NAME]", "" ),
	SET_ELEM( "[FIELDSET_NAME]", "" ),
	ENTRY( "[ENTRY_NAME]", "" ),
	// NOTE these are currently integers; using number for forward compatibility
	VAL_EXPECTED( "[EXPECTED]", ",number" ),
	VAL_ACTUAL( "[ACTUAL]", ",number" ),
	VAL_MIN( "[MINIMUM]", ",number" ),
	VAL_MAX( "[MAXIMUM]", ",number" );
	
	private String textForm;
	private String supplement;


	private EnoLocaleVariables( String textVersion, String typeInfo )
	{
		textForm = textVersion;
		supplement = typeInfo;
	}


	public boolean hasTypeInfo()
	{
		return ! supplement.isEmpty();
	}


	public String getTextForm()
	{
		return textForm;
	}

	public String getSupplement()
	{
		return supplement;
	}

}


















