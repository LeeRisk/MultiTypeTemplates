package me.drakeet.plugin.multitype;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.actions.JavaCreateTemplateInPackageAction;
import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.valueOf;

/**
 * @author drakeet
 */
public class CreateItemFilesAction extends JavaCreateTemplateInPackageAction<PsiClass>
    implements DumbAware {

    private static final String ITEM_TEMPLATE_NAME = "Item";
    private static final String ITEM_VIEW_PROVIDER_TEMPLATE_NAME = "ItemViewProvider";


    public CreateItemFilesAction() {
        super("", IdeBundle.message("action.create.new.class.description"),
            PlatformIcons.CLASS_ICON, true);
    }


    @Override
    protected void buildDialog(final Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder
            .setTitle("Create Item and ItemViewProvider")
            .addKind("Class", PlatformIcons.CLASS_ICON,
                JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME);

        builder.setValidator(new InputValidatorEx() {
            @Override
            public String getErrorText(String inputString) {
                if (inputString.length() > 0 &&
                    !PsiNameHelper.getInstance(project).isQualifiedName(inputString)) {
                    return "This is not a valid Java qualified name";
                }
                return null;
            }


            @Override
            public boolean checkInput(String inputString) {
                return true;
            }


            @Override
            public boolean canClose(String inputString) {
                return !StringUtil.isEmptyOrSpaces(inputString) &&
                    getErrorText(inputString) == null;
            }
        });
    }


    @Override
    protected String removeExtension(String templateName, String className) {
        return StringUtil.trimEnd(className, ".java");
    }


    @Override
    protected String getErrorTitle() {
        return IdeBundle.message("title.cannot.create.class");
    }


    @Override
    protected String getActionName(PsiDirectory directory, String newName, String templateName) {
        return IdeBundle.message("progress.creating.class", StringUtil.getQualifiedName(
            JavaDirectoryService.getInstance().getPackage(directory).getQualifiedName(), newName));
    }


    protected final PsiClass doCreate(PsiDirectory dir, String className, String templateName)
        throws IncorrectOperationException {
        PsiClass result = JavaDirectoryService.getInstance()
            .createClass(dir, className + "ViewProvider",
                ITEM_VIEW_PROVIDER_TEMPLATE_NAME);
        JavaDirectoryService
            .getInstance().createClass(dir, className, ITEM_TEMPLATE_NAME);
        onProcessItemViewProvider(dir, className, result);
        return result;
    }


    private void onProcessItemViewProvider(final PsiDirectory dir, final String itemName, final PsiClass itemClass) {
        final Document document = PsiDocumentManager.getInstance(itemClass.getProject())
            .getDocument(itemClass.getContainingFile());
        char firstChar = itemName.charAt(0);
        String stringFirst = valueOf(firstChar).toLowerCase();

        final String varItemName;
        if (itemName.length() > 1) {
            varItemName = stringFirst + itemName.substring(1, itemName.length());
        } else {
            varItemName = stringFirst;
        }
        new WriteCommandAction.Simple(itemClass.getProject()) {
            @Override protected void run() throws Throwable {
                document.setText(document.getText()
                    .replace("MULTITYPE_TEMPLATE_ITEM_CLASS", itemName)
                    .replace("MULTITYPE_TEMPLATE_ITEM_NAME", varItemName));
                CodeStyleManager.getInstance(itemClass.getProject()).reformat(itemClass);
            }
        }.execute();
    }


    @Override
    protected PsiElement getNavigationElement(@NotNull PsiClass createdElement) {
        return createdElement.getLBrace();
    }


    @Override
    protected void postProcess(PsiClass createdElement, String templateName, Map<String, String> customProperties) {
        super.postProcess(createdElement, templateName, customProperties);

        moveCaretAfterNameIdentifier(createdElement);
    }
}
