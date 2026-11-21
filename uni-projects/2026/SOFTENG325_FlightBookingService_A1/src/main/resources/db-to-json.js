const fs = require('fs'); // File system for reading and writing files

// Define file paths
const inputFilePath = './db-init.sql'; // Replace with the path to your #thisFile
const outputFilePath = './db-contents.json'; // Output JSON file

// Function to extract table data
function extractData(scriptContent, tableName) {
    const regex = new RegExp(
        `INSERT INTO ${tableName} \\((.*?)\\) VALUES \\((.*?)\\);`,
        'g'
    );
    const data = [];
    let match;

    while ((match = regex.exec(scriptContent)) !== null) {
        const columns = match[1].split(',').map(col => col.trim());
        const values = match[2]
            .split(',')
            .map(val => val.trim().replace(/^'|'$/g, '')); // Trim quotes

        // Map columns to values
        const entry = {};
        columns.forEach((col, index) => {
            entry[camelCase(col)] = isNaN(values[index])
                ? values[index]
                : parseFloat(values[index]);
        });
        data.push(entry);
    }

    return data;
}

// Helper function to convert snake_case to camelCase
function camelCase(str) {
    return str
        .toLowerCase()
        .replace(/_([a-z])/g, group => group[1].toUpperCase());
}

// Main function to read the file, extract data, and save to JSON
function processSQLFile() {
    try {
        // Read the SQL file
        const scriptContent = fs.readFileSync(inputFilePath, 'utf-8');

        // Extract airport and flight data
        const airports = extractData(scriptContent, 'AIRPORT');
        const aircraftTypes = extractData(scriptContent, 'AIRCRAFT_TYPE');
        const flights = extractData(scriptContent, 'FLIGHT');

        // Combine data into a single JSON output
        // const outputData = {
        //     airports,
        //     aircraftTypes,
        //     flights
        // };
        const outputData = [...flights];
        outputData.forEach(flight => {
           flight.origin = airports.find(airport => airport.id === flight.originId);
           delete flight.originId;
           flight.destination = airports.find(airport => airport.id === flight.destinationId);
           delete flight.destinationId;
           flight.aircraftType = aircraftTypes.find(at => at.id === flight.aircraftTypeId);
           delete flight.aircraftTypeId;
        });

        // Write the output to a JSON file
        fs.writeFileSync(outputFilePath, JSON.stringify(outputData, null, 2));
        console.log(`Data extraction complete. Output saved to ${outputFilePath}`);
    } catch (error) {
        console.error('Error processing the file:', error);
    }
}

// Run the script
processSQLFile();