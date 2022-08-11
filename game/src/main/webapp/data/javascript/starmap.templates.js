const templateUserFn = user =>
    `<div style="display:flex;flex-direction:row;justify-content:space-between;">
                     <span>${user.name}</span>
                     <span>${user.shipcount}</span>
                 </div>
     <ul class="shipclasses toggleContent user-sectordata">
     					${templateAllShiptypesFn(user.shiptypes)}
                 `;


 const templateShiptypeFn = shiptype => `
    <li class="ng-scope shiptypetoggle">
        <div class="shiptype">
            ${shiptype.count}x ${shiptype.name}
            <a href="./ds?module=schiffinfo&amp;ship=${shiptype.id}">
                <img class="schiffstypengrafik" src="${shiptype.picture}">
            </a>
        </div>
        <table id="${shiptype.userId}-${shiptype.id}" style="display:none;">
            ${templateAllShipsFn(shiptype.ships)}
        </table>
    </li>
 `;

 const templateAllShiptypesFn = data => /*html*/
     data.map(list => { return templateShiptypeFn(list) + "\n" }).join("");



  const templateAllShipsFn = data => /*html*/
      data.map(list => { return templateShipFn(list) + "\n" }).join("");


const templateShipFn = ship => `
<tr>
    <td class="name">
        <a href="./ds?module=schiff&amp;action=default&amp;ship=${ship.id}">${ship.name}</a>
        <!-- uiIf: !user.eigener -->
    </td>
    <td class="status">
        <!-- uiIf: ship.maxGedockt>0 -->
        <span title="gedockte Schiffe">
            <img alt="" src="./data/interface/schiffe/${ship.ownerrace}/icon_container.gif">
            ${ship.gedockt}/${ship.maxGedockt}
        </span>
        <!-- uiIf: ship.maxGelandet>0 --><span title="gelandete Schiffe">
            <img alt="" src="./data/interface/schiffe/${ship.ownerrace}/icon_schiff.gif">
            ${ship.gelandet}/${ship.maxGelandet}
        </span>
        <!-- uiIf: ship.maxEnergie>0 --><span title="Energie">
            <img alt="" src="./data/interface/energie.gif">
            ${ship.energie}/${ship.maxEnergie}
        </span>
        <!-- uiIf: ship.ueberhitzung>${ship.ueberhitzung} -->
    </td>
    <td class="aktionen">
        <!-- uiIf: ship.kannFliegen --><a ui-if="ship.kannFliegen" title="Schiff bewegen"  href="#">
            <img class="can-fly" id="s-${ship.id}" ng-src="./data/interface/move.svg" alt="" src="./data/interface/move.svg">
        </a>
    </td>
</tr>
`;

const templateTileFn = data => /*html*/
    `<div style="position:absolute;width:500px;height:500px;top:${500*data.y}px;left:${500*data.x}px">
         <img src="${data.url}")/>
     </div>`;


const templateScansector = data => /*html*/
    `<div id="scanship-${data.shipId}" class="scanrange scanrange${data.scanRange +1}" style="position: absolute; top: ${(((data.location.y - 1) * 25) + 12.5)}px; left: ${(((data.location.x - 1) * 25) + 12.5)}px; background-color: white; "></div>`;
    //`<div id="scanship-${data.shipId}" class="scanrange scanrange${data.scanRange +1}" style="background-color: white;grid-column-start:${data.location.x-data.scanRange};grid-column-end:${data.location.x+data.scanRange};grid-row-start:${data.location.y-data.scanRange};grid-row-end:${data.location.y+data.scanRange}; "></div>`;


//const templateScannedSector = data => `<div onclick="loadSectorData(${data.x}, ${data.y}, ${data.scanner})" style="width:25px;height:25px;position:absolute;top:${data.y*25-25}px;left:${data.x*25-25}px;${data.bg != null && data.bg.image != undefined ? 'background-image:url('+data.bg.image+')' : ''}">
const templateScannedSector = data => `<div onclick="loadSectorData(${data.x}, ${data.y}, ${data.scanner})" style="grid-column-start:${data.x};grid-column-end:${data.x};grid-row-start:${data.y};grid-row-end:${data.y};${data.bg != null && data.bg.image != undefined ? 'background-image:url('+data.bg.image+')' : ''}">
${data.fg!=null ? '<img src="'+data.fg+'"/>' : ''}
</div>`;