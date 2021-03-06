package me.panavtec.presentation.compiler.proxyviews;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import me.panavtec.presentation.common.views.qualifiers.NoDecorate;
import me.panavtec.presentation.common.views.qualifiers.ThreadDecoratedView;
import me.panavtec.presentation.compiler.proxyviews.model.EnclosingView;
import me.panavtec.presentation.compiler.proxyviews.model.ViewMethod;
import me.panavtec.presentation.compiler.proxyviews.writer.ViewWriter;
import me.panavtec.presentation.compiler.tools.ElementTools;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ViewAnnotationProcessor extends AbstractProcessor {

  private boolean firstProcessing;
  private ElementTools elementTools = new ElementTools();
  private ViewWriter writer = new ViewWriter();

  @Override public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    firstProcessing = true;
  }

  @Override public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    System.out.println("#######################");
    System.out.println("Starting View Processor");
    System.out.println("#######################");
    if (!firstProcessing) {
      return false;
    }
    firstProcessing = false;
    Collection<EnclosingView> enclosingOutputs = processAnnotations(roundEnv);
    writer.write(enclosingOutputs, processingEnv.getFiler());
    return true;
  }

  private Collection<EnclosingView> processAnnotations(RoundEnvironment roundEnv) {
    Set<? extends Element> views = roundEnv.getElementsAnnotatedWith(ThreadDecoratedView.class);
    ArrayList<EnclosingView> modelViews = new ArrayList<>();
    for (Element e : views) {
      modelViews.add(processView(e));
    }
    return modelViews;
  }

  private EnclosingView processView(Element elementView) {
    EnclosingView enclosingView = new EnclosingView();
    enclosingView.setClassName(elementTools.getElementClassName(elementView));
    enclosingView.setPackageName(elementTools.getElementPackagename(elementView));

    System.out.println("Processing: " + elementView.toString());
    processMethodsOfView(enclosingView, elementView);

    List<? extends TypeMirror> extendsViewInterfaces = ((TypeElement) elementView).getInterfaces();
    for (TypeMirror mirror : extendsViewInterfaces) {
      processMethodsOfView(enclosingView, processingEnv.getTypeUtils().asElement(mirror));
    }

    return enclosingView;
  }

  private void processMethodsOfView(EnclosingView enclosingView, Element view) {
    List<? extends Element> enclosedElements = view.getEnclosedElements();
    for (Element e : enclosedElements) {
      System.out.println("Method process: " + e.toString());
      ViewMethod viewMethod = new ViewMethod();
      viewMethod.setMethodName(elementTools.getFieldName(e));
      viewMethod.setReturnType(((ExecutableElement) e).getReturnType());
      viewMethod.setDecorate(e.getAnnotation(NoDecorate.class) == null);
      List<? extends VariableElement> parameters = ((ExecutableElement) e).getParameters();
      for (VariableElement parameterElement : parameters) {
        viewMethod.getParameters().add(parameterElement.asType());
      }
      enclosingView.getMethods().add(viewMethod);
    }
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> supportTypes = new LinkedHashSet<>();
    supportTypes.add(ThreadDecoratedView.class.getCanonicalName());
    supportTypes.add(NoDecorate.class.getCanonicalName());
    return supportTypes;
  }
}
