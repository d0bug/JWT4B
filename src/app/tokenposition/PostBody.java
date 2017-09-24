package app.tokenposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import app.helpers.ConsoleOut;
import app.helpers.KeyValuePair;
import app.helpers.TokenCheck;

public class PostBody extends ITokenPosition {
	private String token;
	private boolean found = false;
	// TODO maybe we could scan all parameters?
	private List<String> tokenKeyWords = Arrays.asList("id_token", "ID_TOKEN", "access_token", "token");

	@Override
	public boolean positionFound() {
		if (isRequest) {
			String body = new String(getBody());
			KeyValuePair postJWT = getJWTFromPostBody(body);
			if (postJWT != null) {
				found = true;
				token = postJWT.getValue();
				return true;
			}
		}
		return false;
	}

	public KeyValuePair getJWTFromPostBody(String body) {
		int from = 0;
		int index = body.indexOf("&") == -1 ? body.length() : body.indexOf("&");
		int parameterCount = StringUtils.countMatches(body, "&") + 1;

		List<KeyValuePair> postParameterList = new ArrayList<KeyValuePair>();
		for (int i = 0; i < parameterCount; i++) {
			String parameter = body.substring(from, index);
			parameter = parameter.replace("&", "");

			String[] parameterSplit = parameter.split(Pattern.quote("="));
			if (parameterSplit.length > 1) {
				String name = parameterSplit[0];
				String value = parameterSplit[1];
				postParameterList.add(new KeyValuePair(name, value));
				from = index;
				index = body.indexOf("&", index + 1);
				if (index == -1) {
					index = body.length();
				}
			}
		}
		for (String keyword : tokenKeyWords) {
			for (KeyValuePair postParameter : postParameterList) {
				if (keyword.equals(postParameter.getName())
						&& TokenCheck.isValidJWT(postParameter.getValue())) {
					return postParameter;
				}
			}
		}
		return null;
	}

	@Override
	public String getToken() {
		return found ? token : "";
	}

	@Override
	public byte[] replaceToken(String newToken) {
		String body = new String(getBody());
		boolean replaced = false;
		// we cannot use the location of parameter, as the body might have changed, thus
		// we need to search for it again
		KeyValuePair postJWT = getJWTFromPostBody(body);
		for (String keyword : tokenKeyWords) {
			if (keyword.equals(postJWT.getName())) {
				String toReplace = postJWT.getNameAsParam() + postJWT.getValue();
				body = body.replace(toReplace, postJWT.getNameAsParam() + newToken);
				replaced = true;
			}
		}
		if (!replaced) {
			ConsoleOut.output("Could not replace token in post body.");
		}
		return getHelpers().buildHttpMessage(getHeaders(), body.getBytes());
	}

}