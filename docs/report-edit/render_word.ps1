$ErrorActionPreference = 'Stop'
$docxPath = 'E:\dynamicmart\docs\report-edit\Bao-cao-DynamicMart-dong-bo.docx'
$pdfPath = 'E:\dynamicmart\docs\report-edit\Bao-cao-DynamicMart-dong-bo.pdf'
$word = $null
$document = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $document = $word.Documents.Open($docxPath, $false, $false)
    foreach ($toc in $document.TablesOfContents) { $toc.Update() }
    $document.Fields.Update() | Out-Null
    foreach ($section in $document.Sections) {
        $section.Headers.Item(1).Range.Fields.Update() | Out-Null
        $section.Footers.Item(1).Range.Fields.Update() | Out-Null
    }
    $document.Repaginate()
    $document.Save()
    $document.ExportAsFixedFormat($pdfPath, 17)
    Write-Output "DOCX=$docxPath"
    Write-Output "PDF=$pdfPath"
    Write-Output "PAGES=$($document.ComputeStatistics(2))"
}
finally {
    if ($document -ne $null) { $document.Close($false) }
    if ($word -ne $null) { $word.Quit() }
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}
