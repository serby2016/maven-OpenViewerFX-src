/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/support/
 *
 * (C) Copyright 1997-2017 IDRsolutions and Contributors.
 *
 * This file is part of JPedal/JPDF2HTML5
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ---------------
 * PageNavigator.java
 * ---------------
 */
package org.jpedal.examples.viewer.commands;

import javax.swing.JOptionPane;
import org.jpedal.PdfDecoderInt;
import org.jpedal.display.Display;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.GUI.PageCounter;
import org.jpedal.gui.GUIFactory;
import org.jpedal.io.TiffHelper;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

/**
 * This class controls the different methods that allow you to navigate a
 * document by page, its critical method is navigatePages()
 */
public class PageNavigator {

    //whether page turn is currently animating
    private static boolean pageTurnAnimating;

    //flag to track if page decoded twice
    private static int lastPageDecoded = -1;

    //Objects required to load Tiff
    private static TiffHelper tiffHelper;

    //Flag to prevent page changing is page changing currently taking place (prevent viewer freezing)
    private static boolean pageChanging;
    
    public static void gotoPage(String page, final GUIFactory currentGUI, final Values commonValues, final PdfDecoderInt decode_pdf) {
        int newPage;

        page = page.split("/")[0];

        //allow for bum values
        try {
            newPage = Integer.parseInt(page);

            //if loading on linearized thread, see if we can actually display
            if (!decode_pdf.isPageAvailable(newPage)) {
                currentGUI.showMessageDialog("Page " + newPage + " is not yet loaded");
                currentGUI.setPageCounterText(PageCounter.PAGECOUNTER2, currentGUI.getPageLabel(commonValues.getCurrentPage()));
                return;
            }

            //adjust for double jump on facing
            if (decode_pdf.getDisplayView() == Display.FACING || decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) {
                if ((decode_pdf.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER) || decode_pdf.getDisplayView() != Display.FACING) && (newPage & 1) == 1 && newPage != 1) {
                    newPage--;
                } else if (!decode_pdf.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER) && (newPage & 1) == 0) {
                    newPage--;
                }
            }

            //allow for invalid value
            if ((newPage > decode_pdf.getPageCount()) | (newPage < 1)) {

                currentGUI.showMessageDialog(Messages.getMessage("PdfViewerPageLabel.text") + ' '
                        + page + ' ' + Messages.getMessage("PdfViewerOutOfRange.text") + ' ' + decode_pdf.getPageCount());

                newPage = commonValues.getCurrentPage();

                currentGUI.setPageNumber();
            }

        } catch (final Exception e) {
            currentGUI.showMessageDialog('>' + page + "< " + Messages.getMessage("PdfViewerInvalidNumber.text")+ ' ' +e);
            newPage = commonValues.getCurrentPage();
            currentGUI.setPageCounterText(PageCounter.PAGECOUNTER2, currentGUI.getPageLabel(commonValues.getCurrentPage()));
        }

        navigatePages(newPage - commonValues.getCurrentPage(), commonValues, decode_pdf, currentGUI);

        if (decode_pdf.getDisplayView() == Display.PAGEFLOW) {
            navigatePages(0, commonValues, decode_pdf, currentGUI);
        }

    }

    public static void goPage(final Object[] args, final GUIFactory currentGUI, final Values commonValues, final PdfDecoderInt decode_pdf) {
        if (args == null) {
            final String page = currentGUI.showInputDialog(Messages.getMessage("PdfViewer.EnterPageNumber"), Messages.getMessage("PdfViewer.GotoPage"), JOptionPane.QUESTION_MESSAGE);
            if (page != null) {
                gotoPage(page, currentGUI, commonValues, decode_pdf);
            }
        } else {
            gotoPage((String) args[0], currentGUI, commonValues, decode_pdf);
        }
    }

    public static void goLastPage(final Object[] args, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {
        if (args == null) {
            if ((commonValues.getSelectedFile() != null) && (commonValues.getPageCount() > 1) && (commonValues.getPageCount() - commonValues.getCurrentPage() > 0)) //					forward(commonValues.getPageCount() - commonValues.getCurrentPage());
            {
                navigatePages(commonValues.getPageCount() - commonValues.getCurrentPage(), commonValues, decode_pdf, currentGUI);
            }
        }
    }

    public static void goFirstPage(final Object[] args, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {
        if (args == null) {
            if ((commonValues.getSelectedFile() != null) && (commonValues.getPageCount() > 1) && (commonValues.getCurrentPage() != 1)) //					back(commonValues.getCurrentPage() - 1);
            {
                navigatePages(-(commonValues.getCurrentPage() - 1), commonValues, decode_pdf, currentGUI);
            }
        }
    }

    public static void goFForwardPage(final Object[] args, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {
        if ((args == null) && (commonValues.getSelectedFile() != null)){
                if (commonValues.getPageCount() < commonValues.getCurrentPage() + 10) //						forward(commonValues.getPageCount()-commonValues.getCurrentPage());
                {
                    navigatePages(commonValues.getPageCount() - commonValues.getCurrentPage(), commonValues, decode_pdf, currentGUI);
                } else {
                    navigatePages(10, commonValues, decode_pdf, currentGUI);
                }
        }
    }

    public static void goForwardPage(final Object[] args, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {
        if (args == null) {
            if (commonValues.getSelectedFile() != null) //forward(1);
            {
                navigatePages(1, commonValues, decode_pdf, currentGUI);
            }
        } else {
            if (commonValues.getSelectedFile() != null) //forward(Integer.parseInt((String) args[0]));
            {
                navigatePages(Integer.parseInt((String) args[0]), commonValues, decode_pdf, currentGUI);
            }
            while (Values.isProcessing()) {
                //Wait while pdf is loading
                try {
                    Thread.sleep(5000);
                } catch (final Exception e) {
                    LogWriter.writeLog("Attempting to set propeties values " + e);
                }
            }
        }
    }

    public static void goBackPage(final Object[] args, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {
        if (args == null) {
            if (commonValues.getSelectedFile() != null) //back(1);
            {
                navigatePages(-1, commonValues, decode_pdf, currentGUI);
            }
        } else {
            if (commonValues.getSelectedFile() != null) //back(Integer.parseInt((String) args[0]));
            {
                navigatePages(-Integer.parseInt((String) args[0]), commonValues, decode_pdf, currentGUI);
            }
            while (Values.isProcessing()) {
                //Wait while pdf is loading
                   try {
                    Thread.sleep(5000);
                } catch (final Exception e) {
                    LogWriter.writeLog("Attempting to set propeties values " + e);           
                }
            }
        }
    }

    public static void goFBackPage(final Object[] args, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {
        if (args == null) {
            if (commonValues.getSelectedFile() != null) {
                if (commonValues.getCurrentPage() <= 10) //back(commonValues.getCurrentPage() - 1);
                {
                    navigatePages(-(commonValues.getCurrentPage() - 1), commonValues, decode_pdf, currentGUI);
                } else //back(10);
                {
                    navigatePages(-10, commonValues, decode_pdf, currentGUI);
                }
            }
        }
    }

    private static int getUpdatedPageNumber(final int displayMode, final int currentPage, final int totalPageCount, int changeCount) {
        //Facing modes need to move at least by 2 pages others page will not change
        if (displayMode == Display.FACING || displayMode == Display.CONTINUOUS_FACING) {

            if (changeCount == -1 && currentPage != 2) {
                changeCount = -2;
            }

            if (changeCount == 1 && currentPage != totalPageCount - 1) {
                changeCount = 2;
            }
        }

        //new page number
        return currentPage + changeCount;
    }

    private static void changePage(final PdfDecoderInt decode_pdf, final GUIFactory currentGUI, final Values commonValues, final int updatedTotal) {
        commonValues.setCurrentPage(updatedTotal);
        
        if (decode_pdf.getDisplayView() == Display.CONTINUOUS
                || decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) {

            currentGUI.decodePage();

            //Added here else number not updated
            currentGUI.setPageNumber();

            pageChanging = false;
            return;
        }

        currentGUI.resetStatusMessage("Loading Page " + commonValues.getCurrentPage());

        //reset as rotation may change!
        decode_pdf.setPageParameters(currentGUI.getScaling(), commonValues.getCurrentPage());

        //decode the page
        if (commonValues.isPDF()) {
            currentGUI.decodePage();
        }
    }
    
    private static void navigatePagePrevious(int count, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {

        int updatedTotal = getUpdatedPageNumber(decode_pdf.getDisplayView(), commonValues.getCurrentPage(), decode_pdf.getPageCount(), count);

        //example code to show how to check if page is now available
        //if loading on linearized thread, see if we can actually display
        if (!decode_pdf.isPageAvailable(updatedTotal)) {
            currentGUI.showMessageDialog("Page " + updatedTotal + " is not yet loaded");
            pageChanging = false;
            return;
        }

        if (!Values.isProcessing()) { //lock to stop multiple accesses

            //if in range update count and decode next page. 
            //Decoded pages are cached so will redisplay almost instantly
            if (updatedTotal <= commonValues.getPageCount()) {

                if (commonValues.isMultiTiff()) {
                    changeTiffPage(commonValues, decode_pdf, currentGUI, count, updatedTotal);
                } else {
                    //adjust for double jump on facing
                    if (decode_pdf.getDisplayView() == Display.FACING || decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) {
                        if (decode_pdf.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER) || decode_pdf.getDisplayView() != Display.FACING) {
                            //updatedTotal++;

                            if (updatedTotal > commonValues.getPageCount()) {
                                updatedTotal = commonValues.getPageCount();
                            }

                            if ((updatedTotal & 1) == 1 && updatedTotal != 1) {
                                updatedTotal--;
                            }

                            if (decode_pdf.getDisplayView() == Display.FACING) {
                                count = ((updatedTotal) / 2) - ((commonValues.getCurrentPage()) / 2);
                            }
                        } else {
                            //updatedTotal++;

                            if ((updatedTotal & 1) == 0) {
                                updatedTotal--;
                            }

                            count = ((updatedTotal + 1) / 2) - ((commonValues.getCurrentPage() + 1) / 2);
                        }
                    }

                    //animate if using drag in facing
                    if (count == 1 && decode_pdf.getDisplayView() == Display.FACING
                            && decode_pdf.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)
                            && decode_pdf.getPageCount() != 2
                            && currentGUI.getPageTurnScalingAppropriate()
                            && updatedTotal / 2 != commonValues.getCurrentPage() / 2
                            && !decode_pdf.getPdfPageData().hasMultipleSizes()
                            && !pageTurnAnimating) {
                        currentGUI.triggerPageTurnAnimation(null, commonValues, updatedTotal, false);
                    } else {
                        changePage(decode_pdf, currentGUI, commonValues, updatedTotal);
                    }
                }
            }
        } else {
            currentGUI.showMessageDialog(Messages.getMessage("PdfViewerDecodeWait.message"));
        }

    }
    
    private static void navigatePageNext(int count, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {

        int updatedTotal = getUpdatedPageNumber(decode_pdf.getDisplayView(), commonValues.getCurrentPage(), decode_pdf.getPageCount(), count);

        //if loading on linearized thread, see if we can actually display
        if (!decode_pdf.isPageAvailable(updatedTotal)) {
            currentGUI.showMessageDialog("Page " + updatedTotal + " is not yet loaded");
            pageChanging = false;
            return;
        }

        if (!Values.isProcessing()) { //lock to stop multiple accesses

            //if in range update count and decode next page. Decoded pages are
            //cached so will redisplay almost instantly
            if (updatedTotal >= 1) {

                if (commonValues.isMultiTiff()) {
                    changeTiffPage(commonValues, decode_pdf, currentGUI, count, updatedTotal);
                } else {

                    //adjust for double jump on facing
                    if (decode_pdf.getDisplayView() == Display.FACING || decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) {
                        if (decode_pdf.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER) || decode_pdf.getDisplayView() != Display.FACING) {
                            if (count == -1) {
                                updatedTotal--;
                            }

                            if (updatedTotal < 1) {
                                updatedTotal = 1;
                            }

                            if ((updatedTotal & 1) == 1 && updatedTotal != 1) {
                                updatedTotal--;
                            }

                            if (decode_pdf.getDisplayView() == Display.FACING) {
                                count = ((updatedTotal) / 2) - ((commonValues.getCurrentPage()) / 2);
                            }
                        } else {
                            if ((updatedTotal & 1) == 0) {
                                updatedTotal--;
                            }

                            if (decode_pdf.getDisplayView() == Display.FACING) {
                                count = ((updatedTotal + 1) / 2) - ((commonValues.getCurrentPage() + 1) / 2);
                            }
                        }
                    }

                    //animate if using drag in facing
                    if (count == -1 && decode_pdf.getDisplayView() == Display.FACING
                            && decode_pdf.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)
                            && currentGUI.getPageTurnScalingAppropriate()
                            && decode_pdf.getPageCount() != 2
                            && (updatedTotal != commonValues.getCurrentPage() - 1 || updatedTotal == 1)
                            && !decode_pdf.getPdfPageData().hasMultipleSizes()
                            && !pageTurnAnimating) {
                        currentGUI.triggerPageTurnAnimation(null, commonValues, updatedTotal, true);
                    } else {
                        changePage(decode_pdf, currentGUI, commonValues, updatedTotal);
                    }
                }
            }
        } else {
            currentGUI.showMessageDialog(Messages.getMessage("PdfViewerDecodeWait.message"));
        }

    }
    
    public static void navigatePages(final int count, final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI) {

        //Check isOpen as failling to load file may allow nav buttons to function
        if (count == 0 || !decode_pdf.isOpen()) {
            return;
        }

        if(!pageChanging){
			pageChanging = true;
			
			if (count > 0) {
                
                navigatePageNext(count, commonValues, decode_pdf, currentGUI);
            
            } else {
                
                navigatePagePrevious(count, commonValues, decode_pdf, currentGUI);
			}

			//Ensure thumbnail scroll bar is updated when page changed
			if (currentGUI.getThumbnailScrollBar() != null) {
				currentGUI.setThumbnailScrollBarValue(commonValues.getCurrentPage() - 1);
			}

			//After changing page, ensure buttons are updated, redundent buttons are hidden
			currentGUI.getButtons().hideRedundentNavButtons(currentGUI);

			currentGUI.setPageNumber();

			pageChanging = false;
		}
    }

    private static void changeTiffPage(final Values commonValues, final PdfDecoderInt decode_pdf, final GUIFactory currentGUI, final int count, final int updatedTotal){
        //Update page number and draw new page
        commonValues.setTiffImageToLoad((lastPageDecoded - 1) + count);
        drawMultiPageTiff(commonValues, decode_pdf);

        //Update Tiff page
        commonValues.setCurrentPage(updatedTotal);
        lastPageDecoded = commonValues.getTiffImageToLoad() + 1;
        currentGUI.setPageNumber();

		//Display new page
        decode_pdf.repaint();


    }
    public static void drawMultiPageTiff(final Values commonValues, final PdfDecoderInt decode_pdf) {

        if (tiffHelper != null) {
            commonValues.setBufferedImg(tiffHelper.getImage(commonValues.getTiffImageToLoad()));

            if (commonValues.getBufferedImg() != null) {
                //flush any previous pages
                decode_pdf.getDynamicRenderer().writeCustom(DynamicVectorRenderer.FLUSH, null);
                decode_pdf.getPages().refreshDisplay();

                Images.addImage(decode_pdf, commonValues);
            }
        }
    }

    public static void setPageTurnAnimating(final boolean a, final GUIFactory currentGUI) {
        pageTurnAnimating = a;

        //disable buttons during animation
        if (a) {
            currentGUI.getButtons().getButton(Commands.FORWARDPAGE).setEnabled(false);
            currentGUI.getButtons().getButton(Commands.BACKPAGE).setEnabled(false);
            currentGUI.getButtons().getButton(Commands.FFORWARDPAGE).setEnabled(false);
            currentGUI.getButtons().getButton(Commands.FBACKPAGE).setEnabled(false);
            currentGUI.getButtons().getButton(Commands.LASTPAGE).setEnabled(false);
            currentGUI.getButtons().getButton(Commands.FIRSTPAGE).setEnabled(false);
        } else {
            currentGUI.getButtons().hideRedundentNavButtons(currentGUI);
        }
    }

    public static boolean getPageTurnAnimating() {
        return pageTurnAnimating;
    }

    public static void setLastPageDecoded(final int x) {
        lastPageDecoded = x;
    }

    public static TiffHelper getTiffHelper() {
        return tiffHelper;
    }

    public static void setTiffHelper(final TiffHelper tiffHelp) {
        tiffHelper = tiffHelp;
    }
   
}
