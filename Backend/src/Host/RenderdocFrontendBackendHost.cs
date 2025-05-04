using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Application.Components;
using JetBrains.Application.Parts;
using JetBrains.IDE;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features.TextControls;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Cpp.Caches;
using JetBrains.ReSharper.Psi.Cpp.Language;
using JetBrains.ReSharper.Psi.Cpp.Tree;
using JetBrains.ReSharper.Psi.Cpp.Util;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Plugins.Renderdoc.Model.FrontendBackend;
using JetBrains.Util;

namespace JetBrains.Rider.Plugins.Renderdoc.Host;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class RenderdocFrontendBackendHost : IStartupActivity
{
  private readonly ISolution mySolution;
  private readonly IPsiFiles myPsiFiles;
  private readonly CppExternalModule myExternalManager;

  public RenderdocFrontendBackendHost(ISolution solution, IPsiFiles psiFiles, CppExternalModule module)
  {
    mySolution = solution;
    myPsiFiles = psiFiles;
    myExternalManager = module;
    var model = solution.GetProtocolSolution().GetRenderdocFrontendBackendModel();
    SetUpModel(model);
  }

  private void SetUpModel(RenderdocFrontendBackendModel model)
  {
    model.OpenFileAndGetMacroCallExpansions.SetAsync(CollectMacrosFromOpenedAndCommittedFile);
  }

  private async Task<List<RdExpandedMacro>> CollectMacrosFromOpenedAndCommittedFile(Lifetime lifetime, string path)
  {
    VirtualFileSystemPath location;
    using (ReadLockCookie.Create())
    {
      location = VirtualFileSystemPath.Parse(path, mySolution.GetInteractionContext());
      mySolution.GetComponent<RiderEditorManager>().OpenFile(location, OpenFileOptions.NormalNoActivate);
    }
    
    return await myPsiFiles.CommitWithRetryBackgroundRead(lifetime, () =>
    {
      var psiFiles = new List<CppFile>();
      if (location.Name.EndsWith(".shader"))
      {
        var projectFile = mySolution.FindProjectItemsByLocation(location).OfType<IProjectFile>().SelectBestProjectFile();
        
        if (projectFile?.ToSourceFile() is not {} sourceFile)
          return new List<RdExpandedMacro>();

        psiFiles.AddRange(sourceFile.GetPsiFiles<CppLanguage>().OfType<CppFile>());
      }
      else
      {
        var cppLocation = new CppFileLocation(myExternalManager, location);
        psiFiles.AddRange(CppFile.GetAllPsiFiles(mySolution, cppLocation).ToEnumerable());
      }
      
      var macro = new List<RdExpandedMacro>();

      foreach (var psiFile in psiFiles)
      {
        foreach (var macroCall in psiFile.Descendants<MacroCall>())
        {
          var textRange = macroCall.GetDocumentRange().TextRange;
          if (!macroCall.IsTopLevel() ||
                              CppMacroUtil.CalculateMacroSubstitutionText(macroCall, false, out _) is not { } substitution) continue;

          macro.Add(new RdExpandedMacro(textRange.StartOffset, textRange.EndOffset, substitution));
        }
      }

      return macro;
    });
  }
}