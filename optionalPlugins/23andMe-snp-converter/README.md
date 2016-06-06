The 23andMe SNP Converter re-formats 23andMe personal genotype results files 
to a BED-format files you can open and visualize in IGB. 

Installing the Converter adds a new menu item to the 
IGB **Tools** menu.

To run the converter:

1. On the IGB  **Tools** menu, select **23andMe SNP Converter** to open the converter window.
2. In the converter window, select a 23andMe data file.
3. Choose a folder where you'd like to save the converted file and type a file name.
4. **Optional** - Select **convert to latest human genome build** if you would like to view your data alongside the latest reference human genome and annotations.


### About genome builds

Older data files from 23andMe 
reference the 2008 version of the reference human genome, also called 
"build 37" and hg37. In 2013, a newer version was released called "build 38" or 
hg38. If your data file references hg37, select **Use reference to upgrade** to 
convert your data to hg38. Note this will add extra time to the conversion process. 

### Credits

UNC Charlotte graduate student Daniel Narmi developed the 23andMe SNP Converter
with NSF Postdoctoral Fellow Nowlan Freese and IGB lead developer David Norris
providing technical, scientific, and design advice.  Source code is available in 
the Integrated Genome Browser git repository.

