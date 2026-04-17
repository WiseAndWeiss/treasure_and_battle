$baseDir = "app\src\main\java\com\example\treasure_and_battle\model"
$srcDir = "app\src\main\java"

New-Item -ItemType Directory -Force -Path "$baseDir\entity"
New-Item -ItemType Directory -Force -Path "$baseDir\profession"
New-Item -ItemType Directory -Force -Path "$baseDir\item"
New-Item -ItemType Directory -Force -Path "$baseDir\skill"

$mappings = @{
    "BattleEntity.java" = "entity"; "Player.java" = "entity"; "Monster.java" = "entity";
    "Profession.java" = "profession"; "Warrior.java" = "profession"; "Ranger.java" = "profession"; "Mage.java" = "profession";
    "Item.java" = "item"; "Equipment.java" = "item"; "Gem.java" = "item"; "Affix.java" = "item"; "Rarity.java" = "item";
    "Skill.java" = "skill"
}

foreach ($file in $mappings.Keys) {
    if (Test-Path "$baseDir\$file") {
        $subPkg = $mappings[$file]
        Move-Item "$baseDir\$file" "$baseDir\$subPkg\"
        $content = Get-Content "$baseDir\$subPkg\$file" -Raw
        $content = $content -replace 'package com\.example\.treasure_and_battle\.model;', "package com.example.treasure_and_battle.model.$subPkg;"
        Set-Content -Path "$baseDir\$subPkg\$file" -Value $content -Encoding UTF8
    }
}

$files = Get-ChildItem -Path $srcDir -Recurse -Filter *.java
foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw
    $changed = $false
    
    foreach ($file in $mappings.Keys) {
        $className = $file.Replace(".java", "")
        $subPkg = $mappings[$file]
        $search = "import com.example.treasure_and_battle.model.$className;"
        $replacement = "import com.example.treasure_and_battle.model.$subPkg.$className;"
        if ($content.Contains($search)) {
            $content = $content.Replace($search, $replacement)
            $changed = $true
        }
    }
    
    if ($content.Contains('import com.example.treasure_and_battle.model.*;')) {
        $wildcards = "import com.example.treasure_and_battle.model.entity.*;
import com.example.treasure_and_battle.model.profession.*;
import com.example.treasure_and_battle.model.item.*;
import com.example.treasure_and_battle.model.skill.*;"
        $content = $content.Replace('import com.example.treasure_and_battle.model.*;', $wildcards)
        $changed = $true
    }
    
    if ($f.FullName -like "*\model\*") {
        $currentPkg = ""
        if ($content -match 'package (com\.example\.treasure_and_battle\.model\.[a-z]+);') {
            $currentPkg = $matches[1]
        }
        
        $importsToAdd = @()
        foreach ($file in $mappings.Keys) {
            $className = $file.Replace(".java", "")
            $subPkg = $mappings[$file]
            $targetPkg = "com.example.treasure_and_battle.model.$subPkg"
            
            if ($currentPkg -ne $targetPkg -and $currentPkg -ne "") {
                if ($content -match "\b$className\b") {
                    $importStr = "import $targetPkg.$className;"
                    if (-not $content.Contains($importStr)) {
                        $importsToAdd += $importStr
                    }
                }
            }
        }
        
        if ($importsToAdd.Count -gt 0) {
            $lines = $content -split "\r\n|\n"
            $newContent = @()
            $added = $false
            foreach ($line in $lines) {
                $newContent += $line
                if (-not $added -and $line -match '^package ') {
                    $newContent += ""
                    foreach ($imp in $importsToAdd) {
                        $newContent += $imp
                    }
                    $added = $true
                }
            }
            $content = $newContent -join "
"
            $changed = $true
        }
    }
    
    if ($changed) {
        Set-Content -Path $f.FullName -Value $content -Encoding UTF8
    }
}
