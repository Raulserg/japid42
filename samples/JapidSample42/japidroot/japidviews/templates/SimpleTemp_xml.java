package japidviews.templates;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews ._tags.*;
import play.mvc.Http.Context.Implicit;
import models.*;
import play.i18n.Lang;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Http.Session;
import play.mvc.Http.Flash;
import japidviews ._layouts.*;
import play.data.validation.Validation;
import java.util.*;
import static cn.bran.japid.util.WebUtils.*;
import controllers.*;
import static cn.bran.japid.util.StringUtils.*;
//
// NOTE: This file was generated from: japidviews/templates/SimpleTemp.xml
// Change to this file will be lost next time the template file is compiled.
//
public class SimpleTemp_xml extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/templates/SimpleTemp.xml";
	{
		putHeader("Content-Type", "text/xml; charset=utf-8");
		setContentType("text/xml; charset=utf-8");
	}

// - add implicit fields with Play

	final Request request = Implicit.request(); 
	final Response response = Implicit.response(); 
	final Session session = Implicit.session();
	final Flash flash = Implicit.flash();
	final Lang lang = Implicit.lang();
	final play.Play _play = new play.Play(); 

// - end of implicit fields with Play 


	public SimpleTemp_xml() {
		super(null);
	}
	public SimpleTemp_xml(StringBuilder out) {
		super(out);
	}
/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"blogTitle",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.SimpleTemp_xml.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String blogTitle; // line 1
	public cn.bran.japid.template.RenderResult render(String blogTitle) {
		this.blogTitle = blogTitle;
		long t = -1;
		try {super.layout();} catch (RuntimeException e) { super.handleException(e);} // line 1
		return new cn.bran.japid.template.RenderResultPartial(getHeaders(), getOut(), t, actionRunners, sourceTemplate);
	}
	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
//------
;// line 1
		p("<xml>\n" + 
"   <p>");// line 1
		p(blogTitle);// line 3
		p("</p>\n" + 
"</xml>");// line 3
		
		endDoLayout(sourceTemplate);
	}

}