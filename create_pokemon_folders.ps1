# PowerShell script to create all Pokemon sprite folders

# Define all Pokemon lines
$pokemonLines = @(
    @("charmander", @("charmander", "charmeleon", "charizard")),
    @("cyndaquil", @("cyndaquil", "quilava", "typhlosion")),
    @("mudkip", @("mudkip", "marshtomp", "swampert")),
    @("piplup", @("piplup", "prinplup", "empoleon")),
    @("snivy", @("snivy", "servine", "serperior")),
    @("froakie", @("froakie", "frogadier", "greninja")),
    @("rowlet", @("rowlet", "dartrix", "decidueye")),
    @("grookey", @("grookey", "thwackey", "rillaboom")),
    @("fuecoco", @("fuecoco", "crocalor", "skeledirge"))
)

# Define evolution stages
$stages = @("basic", "stage_1", "stage_2")

# Define Pokemon states
$states = @("content", "happy", "sad", "thriving", "concerned", "neglected")

$basePath = "src/main/resources/pokemon/sprites"

foreach ($line in $pokemonLines) {
    $lineFolder = $line[0]
    $pokemon = $line[1]
    
    Write-Host "Creating folders for $lineFolder line..."
    
    for ($i = 0; $i -lt $stages.Length; $i++) {
        $stage = $stages[$i]
        $pokemonName = $pokemon[$i]
        
        foreach ($state in $states) {
            $folderPath = "$basePath/$lineFolder/$stage/$state"
            
            # Create the directory
            New-Item -Path $folderPath -ItemType Directory -Force | Out-Null
            
            # Create placeholder files for frame1.png and frame2.png
            $frame1Path = "$folderPath/frame1.png"
            $frame2Path = "$folderPath/frame2.png"
            
            "# Placeholder for $pokemonName $stage $state frame 1" | Out-File -FilePath $frame1Path -Encoding UTF8
            "# Placeholder for $pokemonName $stage $state frame 2" | Out-File -FilePath $frame2Path -Encoding UTF8
        }
    }
}

Write-Host "All Pokemon sprite folders created successfully!"
Write-Host "Total structure:"
Write-Host "- 9 Pokemon lines"
Write-Host "- 3 evolution stages each"
Write-Host "- 6 animation states each"
Write-Host "- 2 frames per state"
Write-Host "= 324 sprite files created"